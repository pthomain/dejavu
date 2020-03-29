/*
 *
 *  Copyright (C) 2017-2020 Pierre Thomain
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package dev.pthomain.android.dejavu.demo.presenter

import android.os.Build.VERSION.SDK_INT
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.google.gson.Gson
import dev.pthomain.android.boilerplate.core.mvp.MvpPresenter
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.boilerplate.core.utils.rx.ioUi
import dev.pthomain.android.dejavu.DejaVu
import dev.pthomain.android.dejavu.demo.DemoActivity
import dev.pthomain.android.dejavu.demo.DemoMvpContract.*
import dev.pthomain.android.dejavu.demo.gson.GsonGlitchFactory
import dev.pthomain.android.dejavu.demo.gson.GsonSerialiser
import dev.pthomain.android.dejavu.demo.model.CatFactResponse
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.CachePriority
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.CachePriority.FreshnessPriority
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.CachePriority.NetworkPriority
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.CachePriority.NetworkPriority.LOCAL_FIRST
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.CachePriority.NetworkPriority.NETWORK_FIRST
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local.Invalidate
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.DoNotCache
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Type
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Type.*
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManagerFactory
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager.Factory.Type.*
import dev.pthomain.android.dejavu.interceptors.response.DejaVuResult
import dev.pthomain.android.glitchy.interceptor.error.glitch.Glitch
import dev.pthomain.android.mumbo.Mumbo
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

internal abstract class BaseDemoPresenter protected constructor(
        demoActivity: DemoActivity,
        protected val uiLogger: Logger
) : MvpPresenter<DemoMvpView, DemoPresenter, DemoViewComponent>(demoActivity),
        DemoPresenter {

    private var instructionType: Type = CACHE
    private var networkPriority: NetworkPriority = LOCAL_FIRST
    private var persistenceType = FILE

    final override var connectivityTimeoutOn: Boolean = true
        set(value) {
            field = value
            dejaVu = newDejaVu()
        }

    final override var useSingle: Boolean = false
    final override var encrypt: Boolean = false
    final override var compress: Boolean = false
    final override var freshness = FreshnessPriority.ANY

    protected val gson = Gson()

    protected var dejaVu: DejaVu<Glitch> = newDejaVu()
        private set

    private fun newDejaVu() = DejaVu.defaultBuilder(context(), GsonSerialiser(gson))
            .withLogger(uiLogger)
            .withPersistence(true, ::pickPersistenceMode)
            .withEncryption(ifElse(SDK_INT >= 23, Mumbo::tink, Mumbo::conceal))
            .withErrorFactory(GsonGlitchFactory())
            .build()

    private fun pickPersistenceMode(persistenceManagerFactory: PersistenceManagerFactory<Glitch>) =
            with(persistenceManagerFactory) {
                when (persistenceType) {
                    FILE -> filePersistenceManagerFactory.create()
                    DATABASE -> databasePersistenceManagerFactory!!.create()
                    MEMORY -> memoryPersistenceManagerFactory.create()
                }
            }

    final override fun loadCatFact(isRefresh: Boolean) {
        instructionType = CACHE
        networkPriority = ifElse(isRefresh, NETWORK_FIRST, LOCAL_FIRST)

        subscribe(
                getResponseObservable(
                        CachePriority.with(networkPriority, freshness),
                        encrypt,
                        compress
                )
        )
    }

    final override fun offline() {
        instructionType = CACHE
        networkPriority = NetworkPriority.LOCAL_ONLY
        subscribe(getOfflineSingle(freshness).toObservable())
    }

    final override fun clearEntries() {
        instructionType = CLEAR
        subscribe(getClearEntriesCompletable().ignoreElements())
    }

    final override fun invalidate() {
        instructionType = INVALIDATE
        subscribe(getInvalidateCompletable().ignoreElements())
    }

    final override fun getCacheOperation() =
                when (instructionType) {
                    CACHE -> Cache(
                            priority = CachePriority.with(networkPriority, freshness),
                            encrypt = encrypt,
                            compress = compress
                    )
                    DO_NOT_CACHE -> DoNotCache
                    INVALIDATE -> Invalidate
                    CLEAR -> Clear()
                }

    private fun subscribe(observable: Observable<out CatFactResponse>) =
            observable.ioUi()
                    .doOnSubscribe { mvpView.onCallStarted() }
                    .doOnError { uiLogger.e(this, it) }
                    .doFinally(::onCallComplete)
                    .autoSubscribe(mvpView::showCatFact)

    private fun subscribe(completable: Completable) =
            completable.ioUi()
                    .doOnSubscribe { mvpView.onCallStarted() }
                    .doFinally(::onCallComplete)
                    .autoSubscribe()

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onDestroy() {
        subscriptions.clear()
    }

    private fun onCallComplete() {
        mvpView.onCallComplete()
//        dejaVu.getStatistics().ioUi()
//                .doOnSuccess { it.log(uiLogger) }
//                .doOnError { uiLogger.e(this, it, "Could not show stats") }
//                .autoSubscribe()
    }

    protected abstract fun getResponseObservable(cachePriority: CachePriority,
                                                 encrypt: Boolean,
                                                 compress: Boolean): Observable<CatFactResponse>

    protected abstract fun getOfflineSingle(freshness: FreshnessPriority): Single<CatFactResponse>
    protected abstract fun getClearEntriesCompletable(): Observable<DejaVuResult<CatFactResponse>>
    protected abstract fun getInvalidateCompletable(): Observable<DejaVuResult<CatFactResponse>>

    companion object {
        internal const val BASE_URL = "https://catfact.ninja/"
        internal const val ENDPOINT = "fact"
    }
}

