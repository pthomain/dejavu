/*
 *
 *  Copyright (C) 2017 Pierre Thomain
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

package dev.pthomain.android.dejavu.interceptors.cache

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.boilerplate.core.utils.rx.single
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority.CacheMode.OFFLINE
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation.Cache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.STALE
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseFactory
import io.reactivex.Observable
import java.util.*

/**
 * Handles the Observable composition according to the each cache operation.
 *
 * @param persistenceManager handles the persistence of the cached responses
 * @param cacheMetadataManager handles the update of the ResponseWrapper metadata
 * @param emptyResponseFactory handles the creation of empty ResponseWrappers for cases where no data can be returned
 * @param dateFactory converts timestamps to Dates
 * @param logger a Logger instance
 */
internal class CacheManager<E>(private val persistenceManager: PersistenceManager<E>,
                               private val cacheMetadataManager: CacheMetadataManager<E>,
                               private val emptyResponseFactory: EmptyResponseFactory<E>,
                               private val dateFactory: (Long?) -> Date,
                               private val logger: Logger)
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * Handles the CLEAR operation
     *
     * @param instructionToken the original request's instruction token
     *
     * @return an Observable emitting an empty ResponseWrapper (with a DONE status)
     */
    fun clearCache(instructionToken: CacheToken) =
            emptyResponseObservable(instructionToken) {
                persistenceManager.clearCache(instructionToken)
            }

    /**
     * Handles the INVALIDATE operation
     *
     * @param instructionToken the original request's instruction token
     *
     * @return an Observable emitting an empty ResponseWrapper (with a DONE status)
     */
    fun invalidate(instructionToken: CacheToken) =
            emptyResponseObservable(instructionToken) {
                persistenceManager.invalidate(instructionToken)
            }

    /**
     * Wraps a callable action into an Observable that only emits an empty ResponseWrapper (with a DONE status).
     *
     * @param instructionToken the original request's instruction token
     * @param action the callable action to execute as an Observable
     *
     * @return an Observable emitting an empty ResponseWrapper (with a DONE status)
     */
    private fun emptyResponseObservable(instructionToken: CacheToken,
                                        action: () -> Unit = {}) =
            Observable.fromCallable(action::invoke).flatMapSingle {
                emptyResponseFactory.emptyResponseWrapper(instructionToken).single()
            }!!

    /**
     * Handles any operation extending of the Expiring type.
     *
     * @param upstream the Observable being composed, typically created by Retrofit and composed by an ErrorInterceptor
     * @see dev.pthomain.android.dejavu.interceptors.error.ErrorInterceptor
     * @param instructionToken the original request's instruction token
     * @param start the time at which the request was made
     *
     * @return an Observable emitting an empty ResponseWrapper (with a DONE status)
     */
    fun getCachedResponse(upstream: Observable<ResponseWrapper<E>>,
                          instructionToken: CacheToken,
                          start: Long) = Observable.defer<ResponseWrapper<E>> {
        val cacheOperation = instructionToken.instruction.operation
        require(cacheOperation is Cache) { "Wrong cache operation: $cacheOperation" }

        val instruction = instructionToken.instruction
        val mode = cacheOperation.priority.mode
        val simpleName = instruction.requestMetadata.responseClass.simpleName

        logger.d(this, "Checking for cached $simpleName")
        val cachedResponse = persistenceManager.getCachedResponse(instructionToken)

        if (cachedResponse != null) {
            logger.d(
                    this,
                    "Found cached $simpleName, status: ${cachedResponse.metadata.cacheToken.status}"
            )
        }

        val diskDuration = (dateFactory(null).time - start).toInt()

        if (mode == OFFLINE) {
            if (cachedResponse == null)
                emptyResponseObservable(instructionToken)
            else
                Observable.just(cachedResponse)
        } else
            getOnlineObservable(
                    cachedResponse,
                    upstream,
                    cacheOperation,
                    instructionToken,
                    diskDuration
            )
    }!!

    //TODO JavaDoc
    private fun getOnlineObservable(cachedResponse: ResponseWrapper<E>?,
                                    upstream: Observable<ResponseWrapper<E>>,
                                    cacheOperation: Cache,
                                    instructionToken: CacheToken,
                                    diskDuration: Int) = Observable.defer<ResponseWrapper<E>> {
        val cachedResponseToken = cachedResponse?.metadata?.cacheToken
        val status = cachedResponseToken?.status
        val simpleName = instructionToken.instruction.requestMetadata.responseClass.simpleName

        if (cachedResponse == null || status == STALE) {
            val fetchAndCache = fetchAndCache(
                    cachedResponse,
                    upstream,
                    cacheOperation,
                    instructionToken,
                    diskDuration
            )

            if (status == STALE && cacheOperation.priority.emitsCachedStale) {
                Observable.concat(
                        Observable.just(cachedResponse).doOnNext {
                            logger.d(this, "Delivering cached $simpleName, status: $status")
                        },
                        fetchAndCache
                )
            } else fetchAndCache
        } else Observable.just(cachedResponse)
    }

    private fun fetchAndCache(previousCachedResponse: ResponseWrapper<E>?,
                              upstream: Observable<ResponseWrapper<E>>,
                              cacheOperation: Cache,
                              instructionToken: CacheToken,
                              diskDuration: Int) =
            Observable.defer<ResponseWrapper<E>> {
                val simpleName = instructionToken.instruction.requestMetadata.responseClass.simpleName
                logger.d(this, "$simpleName is STALE, attempting to refresh")

                upstream
                        .map {
                            cacheMetadataManager.setNetworkCallMetadata(
                                    it,
                                    cacheOperation,
                                    previousCachedResponse,
                                    instructionToken,
                                    diskDuration
                            )
                        }
                        .map { wrapper: ResponseWrapper<E> ->
                            if (wrapper.metadata.exception != null) {
                                logger.e(
                                        this,
                                        wrapper.metadata.exception!!,
                                        "An error occurred fetching $simpleName"
                                )
                                wrapper
                            } else {
                                logger.d(this, "Finished fetching $simpleName, now caching")
                                try {
                                    persistenceManager.cache(
                                            wrapper,
                                            previousCachedResponse
                                    )
                                } catch (e: Exception) {
                                    return@map cacheMetadataManager.setSerialisationFailedMetadata(
                                            wrapper,
                                            e
                                    )
                                }

                                logger.d(this, "Finished caching $simpleName, now delivering")
                                wrapper
                            }
                        }
            }

}

