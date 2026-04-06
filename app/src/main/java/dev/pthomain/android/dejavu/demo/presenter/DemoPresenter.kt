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

import dev.pthomain.android.dejavu.utils.Logger
import dev.pthomain.android.dejavu.demo.AndroidLogger
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Simplified presenter handling cache operations and UI updates.
 * Manages Retrofit client creation and cache configuration.
 */
internal class DemoPresenter(
        private val activity: DemoActivity,
        private val onLogOutput: (String) -> Unit
) {

    private val logger: Logger = AndroidLogger(activity.packageName)
    private val dejaVuFactory = DejaVuFactory(logger, activity)
    private val scope = CoroutineScope(Dispatchers.Main + Job())

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
            collectData(getAnnotationDataFlow(isRefresh))
        } else {
            collectResult(
                    clients.headerClient.execute(getCacheOperation())
                            .map { toDataOrThrow(it) }
            )
        }
    }

    fun offline() {
        instructionType = CACHE
        behaviour = OFFLINE

        if (useAnnotations) {
            val flow = if (freshness == FRESH_ONLY)
                clients.annotationClient.offlineFreshOnly()
            else
                clients.annotationClient.offline()
            collectData(flow)
        } else {
            collectResult(
                    clients.headerClient.execute(getCacheOperation())
                            .map { toDataOrThrow(it) }
            )
        }
    }

    fun clearEntries() {
        instructionType = CLEAR
        if (useAnnotations) {
            collectResult(clients.annotationClient.clearCache())
        } else {
            collectResult(clients.headerClient.execute(Clear()))
        }
    }

    fun invalidate() {
        instructionType = INVALIDATE
        if (useAnnotations) {
            collectResult(clients.annotationClient.invalidate())
        } else {
            collectResult(clients.headerClient.execute(Invalidate))
        }
    }

    private fun getAnnotationDataFlow(isRefresh: Boolean): Flow<CatFactResponse> {
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

    private fun toDataOrThrow(result: DejaVuResult<CatFactResponse>): CatFactResponse =
            when (result) {
                is Response<CatFactResponse, *> -> result.response
                is Empty<*, *, *> -> throw result.exception
                is Result<*, *> -> throw IllegalStateException("Unexpected result type")
            }

    private fun collectData(flow: Flow<CatFactResponse>) {
        scope.launch {
            flow.flowOn(Dispatchers.IO)
                    .onStart { activity.onCallStarted() }
                    .catch { logger.e(this@DemoPresenter, it) }
                    .onCompletion { activity.onCallComplete() }
                    .collect { activity.showCatFact(it) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun collectResult(flow: Flow<DejaVuResult<CatFactResponse>>) {
        scope.launch {
            flow.flowOn(Dispatchers.IO)
                    .onStart { activity.onCallStarted() }
                    .catch { logger.e(this@DemoPresenter, it) }
                    .onCompletion { activity.onCallComplete() }
                    .collect { activity.showResult(it) }
        }
    }

    fun onDestroy() {
        scope.cancel()
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
