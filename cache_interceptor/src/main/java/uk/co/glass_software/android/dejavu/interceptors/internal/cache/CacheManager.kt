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
import io.reactivex.rxkotlin.subscribeBy
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.boilerplate.utils.rx.On
import uk.co.glass_software.android.boilerplate.utils.rx.schedule
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Offline
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Refresh
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.DatabaseManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus.*
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.response.EmptyResponseFactory
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import java.util.*


internal class CacheManager<E>(private val databaseManager: DatabaseManager<E>,
                               private val emptyResponseFactory: EmptyResponseFactory<E>,
                               private val dateFactory: (Long?) -> Date,
                               private val defaultDurationInMillis: Long,
                               private val logger: Logger)
        where E : Exception,
              E : NetworkErrorProvider {

    fun clearCache(instructionToken: CacheToken,
                   typeToClear: Class<*>?,
                   clearOlderEntriesOnly: Boolean) = emptyResponseObservable(instructionToken) {
        databaseManager.clearCache(typeToClear, clearOlderEntriesOnly)
    }

    fun invalidate(instructionToken: CacheToken) = emptyResponseObservable(instructionToken) {
        databaseManager.invalidate(instructionToken)
    }

    private fun emptyResponseObservable(instructionToken: CacheToken,
                                        action: () -> Unit) =
            Observable.fromCallable(action::invoke).flatMap {
                emptyResponseFactory.emptyResponseWrapperObservable(instructionToken)
            }!!

    fun getCachedResponse(upstream: Observable<ResponseWrapper<E>>,
                          instructionToken: CacheToken,
                          cacheOperation: Expiring,
                          start: Long)
            : Observable<ResponseWrapper<E>> {

        val instruction = instructionToken.instruction
        val simpleName = instruction.responseClass.simpleName
        val isRefreshFreshOnly = cacheOperation is Refresh && cacheOperation.freshOnly

        logger.d(this, "Checking for cached $simpleName")
        val cachedResponse = databaseManager.getCachedResponse(instructionToken, start)

        val diskDuration = cachedResponse?.metadata?.callDuration?.disk
                ?: (System.currentTimeMillis() - start).toInt()

        return if (cacheOperation is Offline) {
            if (cachedResponse == null)
                emptyResponseFactory.emptyResponseWrapperObservable(instructionToken)
            else
                Observable.just(cachedResponse)
        } else
            getOnlineObservable(
                    cachedResponse,
                    upstream,
                    cacheOperation,
                    instructionToken,
                    diskDuration,
                    isRefreshFreshOnly,
                    simpleName
            )
    }

    private fun getOnlineObservable(cachedResponse: ResponseWrapper<E>?,
                                    upstream: Observable<ResponseWrapper<E>>,
                                    cacheOperation: Expiring,
                                    instructionToken: CacheToken,
                                    diskDuration: Int,
                                    isRefreshFreshOnly: Boolean,
                                    simpleName: String) =
            if (cachedResponse == null)
                fetchAndCache(
                        null,
                        upstream,
                        cacheOperation,
                        instructionToken,
                        diskDuration,
                        isRefreshFreshOnly
                )
            else {
                val metadata = cachedResponse.metadata
                val cachedResponseToken = metadata.cacheToken
                val status = cachedResponseToken.status

                logger.d(this, "Found cached $simpleName, status: $status")

                if (status === STALE) {
                    refreshStale(
                            cachedResponse,
                            diskDuration,
                            cacheOperation,
                            upstream
                    )
                } else Observable.just(cachedResponse)
            }

    private fun fetchAndCache(previousCachedResponse: ResponseWrapper<E>?,
                              upstream: Observable<ResponseWrapper<E>>,
                              cacheOperation: Expiring,
                              instructionToken: CacheToken,
                              diskDuration: Int,
                              isRefreshFreshOnly: Boolean)
            : Observable<ResponseWrapper<E>> {

        val simpleName = instructionToken.instruction.responseClass.simpleName
        logger.d(this, "Fetching and caching new $simpleName")

        return upstream
                .doOnNext { responseWrapper ->
                    logger.d(this, "Finished fetching $simpleName, now delivering")
                    val metadata = responseWrapper.metadata

                    responseWrapper.metadata =
                            if (metadata.exception == null) {
                                val fetchDate = dateFactory(null)
                                val timeToLiveInMs = cacheOperation.durationInMillis
                                        ?: defaultDurationInMillis
                                val expiryDate = dateFactory(fetchDate.time + timeToLiveInMs)

                                val (encryptData, compressData) = databaseManager.wasPreviouslyEncrypted(
                                        previousCachedResponse,
                                        cacheOperation
                                )

                                val cacheToken = CacheToken.caching(
                                        instructionToken,
                                        compressData,
                                        encryptData,
                                        fetchDate,
                                        fetchDate,
                                        expiryDate
                                )

                                metadata.copy(
                                        cacheToken = if (isRefreshFreshOnly) cacheToken.copy(status = REFRESHED)
                                        else cacheToken,
                                        callDuration = getRefreshCallDuration(metadata.callDuration, diskDuration)
                                )
                            } else {
                                metadata.copy(
                                        cacheToken = metadata.cacheToken.copy(status = EMPTY),
                                        callDuration = getRefreshCallDuration(metadata.callDuration, diskDuration)
                                )
                            }
                }
                .doAfterNext { response ->
                    if (response.metadata.exception == null) {
                        logger.d(this, "$simpleName successfully delivered, now caching")
                        databaseManager.cache(
                                instructionToken,
                                cacheOperation,
                                response,
                                previousCachedResponse
                        ).subscribeBy(
                                onError = { logger.e(this, it, "Could not cache $simpleName") }
                        )
                    }
                }
    }

    private fun getRefreshCallDuration(callDuration: CacheMetadata.Duration,
                                       diskDuration: Int) =
            callDuration.copy(
                    disk = diskDuration,
                    network = callDuration.network - diskDuration
            )

    private fun refreshStale(previousCachedResponse: ResponseWrapper<E>,
                             diskDuration: Int,
                             refreshOperation: Expiring,
                             upstream: Observable<ResponseWrapper<E>>)
            : Observable<ResponseWrapper<E>> {

        val metadata = previousCachedResponse.metadata
        val cacheToken = metadata.cacheToken
        val simpleName = cacheToken.instruction.responseClass.simpleName

        logger.d(this, "$simpleName is ${cacheToken.status}, attempting to refresh")

        val fetchAndCache = fetchAndCache(
                previousCachedResponse,
                upstream,
                refreshOperation,
                cacheToken,
                diskDuration,
                false
        ).map { responseWrapper ->
            val error = responseWrapper.metadata.exception

            if (error != null) {
                val isCouldNotRefresh = error.isNetworkError() && !refreshOperation.freshOnly
                previousCachedResponse.copy(
                        response = if (isCouldNotRefresh) previousCachedResponse.response else null,
                        metadata = updateRefreshStatus(
                                responseWrapper.metadata,
                                error,
                                if (isCouldNotRefresh) COULD_NOT_REFRESH else EMPTY
                        )
                )
            } else {
                updateRefreshed(responseWrapper, REFRESHED)
            }
        }

        return Observable.just(Observable.just(previousCachedResponse), fetchAndCache)
                .concatMap { observable -> observable.schedule(On.Io, On.Trampoline, false) }
                as Observable<ResponseWrapper<E>>
    }

    private fun updateRefreshed(response: ResponseWrapper<E>,
                                newStatus: CacheStatus): ResponseWrapper<E> {
        val simpleName = response.metadata.cacheToken.instruction.responseClass.simpleName
        response.metadata = updateRefreshStatus(
                response.metadata,
                null,
                newStatus
        )

        logger.d(this, "Delivering $simpleName, status: $newStatus")

        return response
    }

    private fun updateRefreshStatus(metadata: CacheMetadata<E>,
                                    exception: E?,
                                    newStatus: CacheStatus) =
            metadata.copy(
                    cacheToken = metadata.cacheToken.copy(status = newStatus),
                    exception = exception
            )
}