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

package dev.pthomain.android.dejavu.demo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.response.Empty
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.response.Result
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.DejaVuFactory
import dev.pthomain.android.dejavu.demo.dejavu.clients.model.CatFactResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CacheResultItem(
    val fact: String?,
    val status: CacheStatus?,
    val source: String,
    val duration: String,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class DemoUiState(
    val isLoading: Boolean = false,
    val results: List<CacheResultItem> = emptyList(),
    val logLines: List<String> = emptyList(),
    val httpClient: HttpClientType = HttpClientType.RETROFIT_ANNOTATION,
    val persistence: PersistenceType = PersistenceType.SQLITE,
    val freshOnly: Boolean = false,
    val encrypt: Boolean = false
)

enum class HttpClientType { RETROFIT_ANNOTATION, RETROFIT_HEADER, KTOR }
enum class PersistenceType { SQLITE, MEMORY }

class DemoViewModel(application: Application) : AndroidViewModel(application) {

    private val logger = AndroidLogger(application.packageName)
    private val factory = DejaVuFactory(logger, application)

    private val _uiState = MutableStateFlow(DemoUiState())
    val uiState: StateFlow<DemoUiState> = _uiState.asStateFlow()

    private fun addLog(message: String) {
        _uiState.update { it.copy(logLines = it.logLines + message) }
    }

    private fun addResult(item: CacheResultItem) {
        _uiState.update { it.copy(results = it.results + item) }
    }

    fun setHttpClient(type: HttpClientType) {
        _uiState.update { it.copy(httpClient = type) }
    }

    fun setPersistence(type: PersistenceType) {
        _uiState.update { it.copy(persistence = type) }
    }

    fun setFreshOnly(value: Boolean) {
        _uiState.update { it.copy(freshOnly = value) }
    }

    fun setEncrypt(value: Boolean) {
        _uiState.update { it.copy(encrypt = value) }
    }

    fun clearLog() {
        _uiState.update { it.copy(results = emptyList(), logLines = emptyList()) }
    }

    fun loadCatFact() {
        executeCacheOperation("LOAD")
    }

    fun refreshCatFact() {
        executeCacheOperation("REFRESH")
    }

    fun goOffline() {
        executeCacheOperation("OFFLINE")
    }

    fun invalidateCache() {
        executeCacheOperation("INVALIDATE")
    }

    fun clearCache() {
        executeCacheOperation("CLEAR")
    }

    private fun executeCacheOperation(operation: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            addLog("-> $operation (${_uiState.value.httpClient}, ${_uiState.value.persistence})")

            try {
                val state = _uiState.value
                val persistence = when (state.persistence) {
                    PersistenceType.SQLITE -> DejaVuFactory.PersistenceType.SQLITE
                    PersistenceType.MEMORY -> DejaVuFactory.PersistenceType.MEMORY
                }
                val clients = factory.createRetrofitClients(persistence, state.encrypt)
                val annotationClient = clients.annotationClient

                val encrypt = state.encrypt
                val freshOnly = state.freshOnly

                val flow: Flow<*> = when (operation) {
                    "LOAD" -> when {
                        encrypt && freshOnly -> annotationClient.getFreshOnlyEncrypted()
                        encrypt -> annotationClient.getEncrypted()
                        freshOnly -> annotationClient.getFreshOnly()
                        else -> annotationClient.get()
                    }
                    "REFRESH" -> when {
                        encrypt && freshOnly -> annotationClient.refreshFreshOnlyEncrypted()
                        encrypt -> annotationClient.refreshEncrypted()
                        freshOnly -> annotationClient.refreshFreshOnly()
                        else -> annotationClient.refresh()
                    }
                    "OFFLINE" -> when {
                        freshOnly -> annotationClient.offlineFreshOnly()
                        else -> annotationClient.offline()
                    }
                    "INVALIDATE" -> annotationClient.invalidate()
                    "CLEAR" -> annotationClient.clearCache()
                    else -> return@launch
                }

                flow.collect { result ->
                    when (result) {
                        is Response<*, *> -> {
                            val response = result.response
                            val token = result.cacheToken
                            addResult(
                                CacheResultItem(
                                    fact = (response as? CatFactResponse)?.fact,
                                    status = token.status,
                                    source = token.status.name,
                                    duration = "${result.callDuration.total}ms"
                                )
                            )
                            addLog("<- ${token.status}: ${(response as? CatFactResponse)?.fact?.take(50)}...")
                        }

                        is Empty<*, *, *> -> {
                            addResult(
                                CacheResultItem(
                                    fact = null,
                                    status = null,
                                    source = "ERROR",
                                    duration = "",
                                    error = result.exception.message
                                )
                            )
                            addLog("x Error: ${result.exception.message}")
                        }

                        is Result<*, *> -> {
                            addResult(
                                CacheResultItem(
                                    fact = null,
                                    status = null,
                                    source = "DONE",
                                    duration = "${result.callDuration.total}ms"
                                )
                            )
                            addLog("<- Operation complete")
                        }

                        is CatFactResponse -> {
                            addResult(
                                CacheResultItem(
                                    fact = result.fact,
                                    status = null,
                                    source = "DIRECT",
                                    duration = ""
                                )
                            )
                            addLog("<- ${result.fact?.take(50)}...")
                        }

                        else -> addLog("<- Result: $result")
                    }
                }
            } catch (e: Exception) {
                addResult(
                    CacheResultItem(
                        fact = null, status = null, source = "ERROR",
                        duration = "", error = e.message
                    )
                )
                addLog("x Exception: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
