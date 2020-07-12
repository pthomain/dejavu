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

package dev.pthomain.android.dejavu.cache

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.metadata.response.CallDuration
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.response.ellapsed
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.STALE
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.cache.metadata.token.getCacheStatus
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Invalidate
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.di.DateFactory
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseFactory
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import io.reactivex.Observable

/**
 * Handles the Observable composition according to the each cache operation.
 *
 * @param persistenceManager handles the persistence of the cached responses
 * @param cacheMetadataManager handles the update of the ResponseWrapper metadata
 * @param emptyResponseFactory handles the creation of empty ResponseWrappers for cases where no data can be returned
 * @param dateFactory converts timestamps to Dates
 * @param logger a Logger instance
 */
internal class CacheManager<E>(
        private val persistenceManager: PersistenceManager,
        private val cacheMetadataManager: CacheMetadataManager<E>,
        private val emptyResponseFactory: EmptyResponseFactory<E>,
        private val dateFactory: DateFactory,
        private val logger: Logger
) where E : Throwable,
        E : NetworkErrorPredicate {

    /**
     * Handles the CLEAR operation
     *
     * @param instructionToken the original request's instruction token
     *
     * @return an Observable emitting an empty ResponseWrapper (with a DONE status)
     */
    fun <R : Any> clearCache(instructionToken: RequestToken<Clear, R>) =
            emptyResponseFactory.createEmptyResponseObservable(instructionToken) {
                with(instructionToken.instruction) {
                    persistenceManager.clearCache(requestMetadata, operation)
                }
            }

    /**
     * Handles the INVALIDATE operation
     *
     * @param instructionToken the original request's instruction token
     *
     * @return an Observable emitting an empty ResponseWrapper (with a DONE status)
     */
    fun <R : Any> invalidate(instructionToken: RequestToken<Invalidate, R>) =
            emptyResponseFactory.createEmptyResponseObservable(instructionToken) {
                persistenceManager.forceInvalidation(instructionToken)
            }

    /**
     * Handles any operation extending of the Expiring type.
     *
     * @param upstream the Observable being composed, typically created by Retrofit and composed by an ErrorInterceptor
     * @see dev.pthomain.android.dejavu.interceptors.error.ErrorInterceptor
     *
     * @return an Observable emitting an empty ResponseWrapper (with a DONE status)
     */
    fun <R : Any> getCachedResponse(
            upstream: Observable<DejaVuResult<R>>,
            requestToken: RequestToken<Cache, R>
    ): Observable<DejaVuResult<R>> =
            Observable.defer {
                val cacheOperation = requestToken.instruction.operation
                val instruction = requestToken.instruction
                val behaviour = cacheOperation.priority.behaviour
                val simpleName = instruction.requestMetadata.responseClass.simpleName

                logger.d(this, "Checking for cached $simpleName")

                val cachedResponse = persistenceManager.get(requestToken)?.run {
                    val status = dateFactory.getCacheStatus(
                            expiryDate,
                            instruction.operation
                    )
                    if (cacheOperation.priority.freshness.isFreshOnly() && !status.isFresh) null
                    else Response(
                            data,
                            ResponseToken(
                                    instruction,
                                    status,
                                    requestToken.requestDate,
                                    expiryDate
                            ),
                            CallDuration(disk = requestToken.ellapsed(dateFactory))
                    )
                }

                if (behaviour.isOffline()) {
                    if (cachedResponse == null)
                        emptyResponseFactory.createEmptyResponseObservable(requestToken)
                    else Observable.just(cachedResponse)
                } else
                    getOnlineObservable(
                            cachedResponse,
                            upstream,
                            cacheOperation,
                            requestToken
                    )
            }

    //TODO JavaDoc
    private fun <R : Any> getOnlineObservable(
            cachedResponse: Response<R, Cache>?,
            upstream: Observable<DejaVuResult<R>>,
            cacheOperation: Cache,
            instructionToken: RequestToken<Cache, R>,
    ) =
            Observable.defer {
                val cachedResponseToken = cachedResponse?.cacheToken
                val status = cachedResponseToken?.status
                val simpleName = instructionToken.instruction.requestMetadata.responseClass.simpleName

                if (cachedResponse == null || status == STALE) {
                    val fetchAndCache = fetchAndCache(
                            cachedResponse,
                            upstream,
                            cacheOperation,
                            instructionToken
                    )

                    if (status == STALE && cacheOperation.priority.freshness.emitsCachedStale) {
                        Observable.concat(
                                Observable.just(cachedResponse).doOnNext {
                                    logger.d(this, "Delivering cached $simpleName, status: $status")
                                },
                                fetchAndCache
                        )
                    } else fetchAndCache
                } else Observable.just(cachedResponse)
            }

    private fun <R : Any> fetchAndCache(
            previousCachedResponse: Response<R, Cache>?,
            upstream: Observable<DejaVuResult<R>>,
            cacheOperation: Cache,
            instructionToken: RequestToken<Cache, R>,
    ) =
            Observable.defer {
                val simpleName = instructionToken.instruction.requestMetadata.responseClass.simpleName
                logger.d(this, "$simpleName is STALE, attempting to refresh")
                val diskDuration = instructionToken.ellapsed(dateFactory)

                upstream.flatMap {
                    if (it is Response<*, *>) {
                        @Suppress("UNCHECKED_CAST")
                        Observable.just(it as Response<R, Cache>)
                                .map {
                                    cacheMetadataManager.setNetworkCallMetadata(
                                            it,
                                            cacheOperation,
                                            previousCachedResponse,
                                            instructionToken,
                                            diskDuration
                                    )
                                }
                                .map { wrapper ->
                                    logger.d(this, "Finished fetching $simpleName, now caching")
                                    try {
                                        val cacheToken = with(wrapper.cacheToken) {
                                            ResponseToken(
                                                    instruction,
                                                    status,
                                                    requestDate,
                                                    dateFactory(requestDate.time + (cacheOperation.durationInSeconds * 1000))
                                            ) //TODO check expiry date etc
                                        }

                                        persistenceManager.put(wrapper.copy(cacheToken = cacheToken))
                                    } catch (e: Exception) {
                                        return@map cacheMetadataManager.setSerialisationFailedMetadata(
                                                wrapper,
                                                e
                                        )
                                    }

                                    logger.d(this, "Finished caching $simpleName, now delivering")
                                    wrapper
                                }
                    } else Observable.just(it)
                }
            }
}
