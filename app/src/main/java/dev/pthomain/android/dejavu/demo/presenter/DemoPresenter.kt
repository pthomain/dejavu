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

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.boilerplate.core.utils.log.SimpleLogger
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.response.Empty
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.response.Result
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.Behaviour
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.Behaviour.OFFLINE
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.Behaviour.ONLINE
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.FreshnessPriority
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.FreshnessPriority.ANY
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.FreshnessPriority.FRESH_ONLY
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Invalidate
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.DoNotCache
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Type.*
import dev.pthomain.android.dejavu.demo.DemoActivity
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.DejaVuFactory
import dev.pthomain.android.dejavu.demo.dejavu.clients.model.CatFactResponse
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Simplified presenter handling cache operations and UI updates.
 * Manages Retrofit client creation and cache configuration.
 */
internal class DemoPresenter(
        private val activity: DemoActivity,
        private val onLogOutput: (String) -> Unit
) {

    private val logger: Logger = SimpleLogger(true, activity.packageName)
    private val dejaVuFactory = DejaVuFactory(logger, activity)
    private val disposables = CompositeDisposable()

    private var instructionType = CACHE
    private var behaviour = ONLINE

    var useAnnotations = true
    var freshness: FreshnessPriority = ANY
    var encrypt = false
        set(value) {
            field = value
            recreateClients()
        }

    var persistence = PersistenceType.SQLITE
        set(value) {
            field = value
            recreateClients()
        }

    private var clients = dejaVuFactory.createRetrofitClients(persistence, encrypt)

    private fun recreateClients() {
        clients = dejaVuFactory.createRetrofitClients(persistence, encrypt)
    }

    fun getCacheOperation(): Operation =
            when (instructionType) {
                CACHE -> Cache(
                        priority = CachePriority.with(behaviour, freshness),
                        serialisation = if (encrypt) "encrypt" else ""
                )
                DO_NOT_CACHE -> DoNotCache
                INVALIDATE -> Invalidate
                CLEAR -> Clear()
            }

    fun loadCatFact(isRefresh: Boolean) {
        instructionType = CACHE
        behaviour = if (isRefresh) Behaviour.INVALIDATE else ONLINE

        if (useAnnotations) {
            subscribeData(getAnnotationDataObservable(isRefresh))
        } else {
            subscribeResult(
                    clients.headerClient.execute(getCacheOperation())
                            .flatMap { toDataOrError(it) }
            )
        }
    }

    fun offline() {
        instructionType = CACHE
        behaviour = OFFLINE

        if (useAnnotations) {
            val single = if (freshness == FRESH_ONLY)
                clients.annotationClient.offlineFreshOnly()
            else
                clients.annotationClient.offline()
            subscribeData(single.toObservable())
        } else {
            subscribeResult(
                    clients.headerClient.execute(getCacheOperation())
                            .flatMap { toDataOrError(it) }
            )
        }
    }

    fun clearEntries() {
        instructionType = CLEAR
        if (useAnnotations) {
            subscribeResult(clients.annotationClient.clearCache())
        } else {
            subscribeResult(clients.headerClient.execute(Clear()))
        }
    }

    fun invalidate() {
        instructionType = INVALIDATE
        if (useAnnotations) {
            subscribeResult(clients.annotationClient.invalidate())
        } else {
            subscribeResult(clients.headerClient.execute(Invalidate))
        }
    }

    private fun getAnnotationDataObservable(isRefresh: Boolean): Observable<CatFactResponse> {
        val client = clients.annotationClient
        return if (isRefresh) {
            when {
                freshness.isFreshOnly() && encrypt -> client.refreshFreshOnlyEncrypted()
                freshness.isFreshOnly() -> client.refreshFreshOnly()
                encrypt -> client.refreshEncrypted()
                else -> client.refresh()
            }
        } else {
            when {
                freshness.isFreshOnly() && encrypt -> client.getFreshOnlyEncrypted()
                freshness.isFreshOnly() -> client.getFreshOnly()
                encrypt -> client.getEncrypted()
                else -> client.get()
            }
        }
    }

    private fun toDataOrError(result: DejaVuResult<CatFactResponse>): Observable<CatFactResponse> =
            when (result) {
                is Response<CatFactResponse, *> -> Observable.just(result.response)
                is Empty<*, *, *> -> Observable.error(result.exception)
                is Result<*, *> -> Observable.empty()
            }

    private fun subscribeData(observable: Observable<CatFactResponse>) {
        val disposable = observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { activity.onCallStarted() }
                .doOnError { logger.e(this, it) }
                .doFinally { activity.onCallComplete() }
                .subscribe(
                        { activity.showCatFact(it) },
                        { logger.e(this, it) }
                )
        disposables.add(disposable)
    }

    @Suppress("UNCHECKED_CAST")
    private fun subscribeResult(observable: Observable<DejaVuResult<CatFactResponse>>) {
        val disposable = observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { activity.onCallStarted() }
                .doOnError { logger.e(this, it) }
                .doFinally { activity.onCallComplete() }
                .subscribe(
                        { activity.showResult(it) },
                        { logger.e(this, it) }
                )
        disposables.add(disposable)
    }

    fun onDestroy() {
        disposables.clear()
    }

    enum class PersistenceType {
        MEMORY,
        SQLITE
    }

    companion object {
        internal const val BASE_URL = "https://catfact.ninja/"
        internal const val ENDPOINT = "fact"
    }
}
