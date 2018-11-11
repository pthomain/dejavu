/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.cache_interceptor.demo.presenter

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import uk.co.glass_software.android.boilerplate.Boilerplate.logger
import uk.co.glass_software.android.boilerplate.ui.mvp.MvpPresenter
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.boilerplate.utils.rx.io
import uk.co.glass_software.android.cache_interceptor.RxCache
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.*
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Expiring.*
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Type.*
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity
import uk.co.glass_software.android.cache_interceptor.demo.DemoMvpContract.*
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse

internal abstract class BaseDemoPresenter protected constructor(private val demoActivity: DemoActivity,
                                                                protected val uiLogger: Logger
) : MvpPresenter<DemoMvpView, DemoPresenter, DemoViewComponent>(demoActivity),
        DemoPresenter {

    private var instructionType: CacheInstruction.Operation.Type = CACHE

    final override var useSingle: Boolean = false
    final override var allowNonFinalForSingle: Boolean = false
    final override var encrypt: Boolean = false
    final override var compress: Boolean = false
    final override var freshOnly: Boolean = false

    protected val gson by lazy { Gson() }

    protected val rxCache by lazy { getRxCache(false) }
    protected val rxCacheNonFinalSingles by lazy { getRxCache(true) }

    private fun getRxCache(allowNonFinalForSingle: Boolean) =
            RxCache.builder()
                    .gson(gson)
                    .mergeOnNextOnError(true)
                    .networkTimeOutInSeconds(5)
                    .allowNonFinalForSingle(allowNonFinalForSingle)
                    .logger(uiLogger)
                    .build(demoActivity)

    private val configuration = rxCache.configuration

    final override fun loadCatFact(isRefresh: Boolean) {
        instructionType = if (isRefresh) REFRESH else CACHE
        subscribe(getResponseObservable(
                isRefresh,
                encrypt,
                compress,
                freshOnly
        ))
    }

    final override fun offline() {
        instructionType = OFFLINE
        subscribe(getOfflineSingle(freshOnly).toObservable())
    }

    final override fun clearEntries() {
        instructionType = CLEAR
        subscribe(getClearEntriesCompletable())
    }

    final override fun invalidate() {
        instructionType = INVALIDATE
        subscribe(getInvalidateCompletable())
    }

    final override fun getCacheInstruction() =
            when (instructionType) {
                CACHE -> Cache(
                        configuration.cacheDurationInMillis,
                        freshOnly,
                        configuration.mergeOnNextOnError,
                        encrypt,
                        compress,
                        false
                )
                REFRESH -> Refresh(
                        configuration.cacheDurationInMillis,
                        freshOnly,
                        configuration.mergeOnNextOnError,
                        false
                )
                DO_NOT_CACHE -> DoNotCache
                INVALIDATE -> Invalidate
                OFFLINE -> Offline(
                        freshOnly,
                        configuration.mergeOnNextOnError
                )
                CLEAR,
                CLEAR_ALL -> Clear(clearOldEntriesOnly = false)
            }.let { operation ->
                CacheInstruction(
                        CatFactResponse::class.java,
                        operation
                )
            }

    private fun subscribe(observable: Observable<out CatFactResponse>) =
            observable
                    .io()
                    .doOnSubscribe { mvpView.onCallStarted() }
                    .doOnComplete(mvpView::onCallComplete)
                    .autoSubscribe(mvpView::showCatFact)

    private fun subscribe(completable: Completable) =
            completable
                    .io()
                    .doOnSubscribe { mvpView.onCallStarted() }
                    .doOnComplete(mvpView::onCallComplete)
                    .autoSubscribe()

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onDestroy() {
        logger.d("Clearing subscriptions")
        subscriptions.clear()
    }

    protected abstract fun getResponseObservable(isRefresh: Boolean,
                                                 encrypt: Boolean,
                                                 compress: Boolean,
                                                 freshOnly: Boolean)
            : Observable<out CatFactResponse>

    protected abstract fun getOfflineSingle(freshOnly: Boolean): Single<out CatFactResponse>
    protected abstract fun getClearEntriesCompletable(): Completable
    protected abstract fun getInvalidateCompletable(): Completable

    companion object {
        internal const val BASE_URL = "https://catfact.ninja/"
        internal const val ENDPOINT = "fact"
    }

}

