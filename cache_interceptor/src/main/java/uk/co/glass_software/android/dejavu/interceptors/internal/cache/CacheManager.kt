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

package uk.co.glass_software.android.dejavu.interceptors.internal.cache

import io.reactivex.Observable
import uk.co.glass_software.android.boilerplate.core.utils.log.Logger
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Offline
import uk.co.glass_software.android.dejavu.configuration.ErrorFactory
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.configuration.Serialiser
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence.PersistenceManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus.*
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.response.EmptyResponseFactory
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.retrofit.annotations.CacheException
import uk.co.glass_software.android.dejavu.retrofit.annotations.CacheException.Type.SERIALISATION
import java.util.*

//TODO JavaDoc
internal class CacheManager<E>(private val errorFactory: ErrorFactory<E>,
                               private val serialiser: Serialiser,
                               private val persistenceManager: PersistenceManager<E>,
                               private val emptyResponseFactory: EmptyResponseFactory<E>,
                               private val dateFactory: (Long?) -> Date,
                               private val defaultDurationInMillis: Long,
                               private val logger: Logger)
        where E : Exception,
              E : NetworkErrorProvider {

    fun clearCache(instructionToken: CacheToken,
                   typeToClear: Class<*>?,
                   clearStaleEntriesOnly: Boolean) = emptyResponseObservable(instructionToken) {
        persistenceManager.clearCache(typeToClear, clearStaleEntriesOnly)
    }

    fun invalidate(instructionToken: CacheToken) = emptyResponseObservable(instructionToken) {
        persistenceManager.invalidate(instructionToken)
    }

    private fun emptyResponseObservable(instructionToken: CacheToken,
                                        action: () -> Unit) =
            Observable.fromCallable(action::invoke).flatMap {
                emptyResponseFactory.emptyResponseWrapperSingle(instructionToken).toObservable()
            }!!

    fun getCachedResponse(upstream: Observable<ResponseWrapper<E>>,
                          instructionToken: CacheToken,
                          cacheOperation: Expiring,
                          start: Long)
            : Observable<ResponseWrapper<E>> {

        val instruction = instructionToken.instruction
        val simpleName = instruction.responseClass.simpleName

        logger.d(this, "Checking for cached $simpleName")
        val cachedResponse = persistenceManager.getCachedResponse(instructionToken, start)

        val diskDuration = cachedResponse
                ?.metadata
                ?.callDuration
                ?.disk
                ?: (dateFactory(null).time - start).toInt()

        return if (cacheOperation is Offline) {
            if (cachedResponse == null)
                emptyResponseFactory.emptyResponseWrapperSingle(instructionToken).toObservable()
            else
                Observable.just(cachedResponse)
        } else
            getOnlineObservable(
                    cachedResponse,
                    upstream,
                    cacheOperation,
                    instructionToken,
                    diskDuration,
                    simpleName
            )
    }

    private fun getOnlineObservable(cachedResponse: ResponseWrapper<E>?,
                                    upstream: Observable<ResponseWrapper<E>>,
                                    cacheOperation: Expiring,
                                    instructionToken: CacheToken,
                                    diskDuration: Int,
                                    simpleName: String): Observable<ResponseWrapper<E>> {
        val cachedResponseToken = cachedResponse?.metadata?.cacheToken
        val status = cachedResponseToken?.status

        if (cachedResponse != null) {
            logger.d(this, "Found cached $simpleName, status: $status")
        }

        return if (cachedResponse == null || status === STALE) {
            val fetchAndCache = fetchAndCache(
                    cachedResponse,
                    upstream,
                    cacheOperation,
                    instructionToken,
                    diskDuration
            )

            return if (status === STALE && !cacheOperation.freshOnly) {
                logger.d(this, "$simpleName is ${cachedResponseToken.status}, attempting to refresh")
                Observable.concat(
                        Observable.just(cachedResponse),
                        fetchAndCache
                )
            } else fetchAndCache
        } else Observable.just(cachedResponse)
    }

    private fun fetchAndCache(previousCachedResponse: ResponseWrapper<E>?,
                              upstream: Observable<ResponseWrapper<E>>,
                              cacheOperation: Expiring,
                              instructionToken: CacheToken,
                              diskDuration: Int)
            : Observable<ResponseWrapper<E>> {

        val simpleName = instructionToken.instruction.responseClass.simpleName
        logger.d(this, "Fetching and caching new $simpleName")

        return upstream
                .map {
                    updateNetworkCallMetadata(
                            simpleName,
                            it,
                            cacheOperation,
                            previousCachedResponse,
                            instructionToken,
                            diskDuration
                    )
                }
                .map {
                    if (!it.metadata.cacheToken.status.isError) {
                        val serialised = serialise(it)

                        if (serialised.metadata.exception != null) {
                            it.copy(
                                    response = null,
                                    metadata = serialised.metadata
                            )
                        } else {
                            persistenceManager.cache(
                                    serialised,
                                    previousCachedResponse
                            )
                            it
                        }
                    } else it
                }
    }

    private fun updateNetworkCallMetadata(simpleName: String,
                                          responseWrapper: ResponseWrapper<E>,
                                          cacheOperation: Expiring,
                                          previousCachedResponse: ResponseWrapper<E>?,
                                          instructionToken: CacheToken,
                                          diskDuration: Int): ResponseWrapper<E> {
        logger.d(this, "Finished fetching $simpleName, now delivering")
        val metadata = responseWrapper.metadata
        val error = responseWrapper.metadata.exception
        val hasError = error != null
        val hasCachedResponse = previousCachedResponse != null
        val previousCacheToken = previousCachedResponse?.metadata?.cacheToken

        val fetchDate = dateFactory(null)
        val cacheDate = if (hasError) previousCacheToken?.cacheDate else fetchDate
        val timeToLiveInMs = cacheOperation.durationInMillis ?: defaultDurationInMillis
        val expiryDate = if (hasError) previousCacheToken?.expiryDate else dateFactory(fetchDate.time + timeToLiveInMs)

        val (encryptData, compressData) = persistenceManager.shouldEncryptOrCompress(
                previousCachedResponse,
                cacheOperation
        )

        val status = if (hasError)
            if (hasCachedResponse) COULD_NOT_REFRESH else EMPTY
        else
            if (hasCachedResponse) REFRESHED else NETWORK

        val cacheToken = CacheToken(
                instructionToken.instruction,
                status,
                compressData,
                encryptData,
                instructionToken.requestMetadata,
                fetchDate,
                if (status == EMPTY) null else cacheDate,
                if (status == EMPTY) null else expiryDate
        )

        val newMetadata = metadata.copy(
                cacheToken,
                callDuration = getRefreshCallDuration(metadata.callDuration, diskDuration)
        )

        return if (status == COULD_NOT_REFRESH)
            responseWrapper.copy(
                    metadata = newMetadata,
                    response = if (cacheOperation.freshOnly) null else previousCachedResponse?.response
            )
        else responseWrapper.copy(metadata = newMetadata)
    }

    private fun serialise(responseWrapper: ResponseWrapper<E>) =
            responseWrapper.response.let { response ->
                val serialised = if (response != null && serialiser.canHandleType(response.javaClass)) {
                    serialiser.serialise(response)
                } else null

                if (serialised == null) {
                    val message = "Could not serialise ${responseWrapper.responseClass.simpleName}: provided serialiser does not support the type. This response will not be cached."
                    logger.e(this, message)

                    val serialisationException = errorFactory.getError(
                            CacheException(SERIALISATION, message)
                    )

                    val serialisationCacheToken = responseWrapper.metadata.cacheToken.let {
                        val newStatus = when (it.status) {
                            NETWORK -> EMPTY
                            REFRESHED -> COULD_NOT_REFRESH
                            else -> it.status
                        }
                        it.copy(
                                status = newStatus,
                                cacheDate = if (newStatus == EMPTY || newStatus == COULD_NOT_REFRESH) null else it.cacheDate,
                                expiryDate = if (newStatus == EMPTY || newStatus == COULD_NOT_REFRESH) null else it.expiryDate
                        )
                    }

                    responseWrapper.copy(
                            response = null,
                            metadata = responseWrapper.metadata.copy(
                                    exception = serialisationException,
                                    cacheToken = serialisationCacheToken
                            )
                    )
                } else responseWrapper.copy(
                        response = serialised,
                        responseClass = String::class.java
                )
            }

    private fun getRefreshCallDuration(callDuration: CacheMetadata.Duration,
                                       diskDuration: Int) =
            callDuration.copy(
                    disk = diskDuration,
                    network = callDuration.network - diskDuration
            )

}