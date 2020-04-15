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

package dev.pthomain.android.dejavu.interceptors.cache

import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CallDuration
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.*
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.responseClass
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.response.Response
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import java.util.*

/**
 * Handles the update of the ResponseWrapper's metadata.
 *
 * @param errorFactory the factory converting exceptions to the custom exception type
 * @param persistenceManager the object in charge of persisting the response
 * @param dateFactory a factory converting timestamps in Dates
 * @param logger a Logger instance
 */
internal class CacheMetadataManager<E>(
        private val errorFactory: ErrorFactory<E>,
        private val persistenceManager: PersistenceManager<E>,
        private val dateFactory: (Long?) -> Date,
        private val durationPredicate: (TransientResponse<*>) -> Int?,
        private val logger: Logger
) where E : Throwable,
        E : NetworkErrorPredicate {

    /**
     * Updates the metadata of a ResponseWrapper just after a network call.
     *
     * @param responseWrapper the wrapper returned from the network call
     * @param cacheOperation the instructed request cache operation
     * @param previousCachedResponse the optional previously cached response for this call
     * @param instructionToken the original request instruction token
     * @param diskDuration the time spent loading the previous response from cache
     *
     * @return the ResponseWrapper updated with the new metadata
     */
    fun <R : Any> setNetworkCallMetadata(
            responseWrapper: Response<R, Cache>,
            cacheOperation: Cache,
            previousCachedResponse: Response<R, Cache>?,
            instructionToken: RequestToken<Cache, R>,
            diskDuration: Int
    ): Response<R, Cache> {
        val hasCachedResponse = previousCachedResponse != null
        val fetchDate = dateFactory(null)
        val status = ifElse(hasCachedResponse, REFRESHED, NETWORK)

        val predicateDuration = if (status.isFresh) {
            durationPredicate(TransientResponse(
                    responseWrapper.response,
                    instructionToken
            ))
        } else null

        val timeToLiveInSeconds = predicateDuration ?: cacheOperation.durationInSeconds
        val expiryDate = dateFactory(fetchDate.time + timeToLiveInSeconds * 1000L)

        val cacheToken = ResponseToken(
                instructionToken.instruction,
                status,
                fetchDate,
                fetchDate,
                expiryDate
        )

        return responseWrapper.copy(
                cacheToken = cacheToken,
                callDuration = getRefreshCallDuration(responseWrapper.callDuration, diskDuration)
        )
    }

    /**
     * Sets the metadata associated with a failure to serialise the response for caching.
     *
     * @param wrapper the wrapper to be cached
     * @param exception the exception that occurred during serialisation
     * @return the response wrapper with the updated metadata
     */
    fun <R : Any> setSerialisationFailedMetadata(
            wrapper: Response<R, Cache>,
            exception: Exception
    ): Response<R, Cache> {
        logger.e(
                this,
                exception,
                "Could not serialise ${wrapper.responseClass().simpleName}: this response will not be cached."
        )

        return with(wrapper.cacheToken) {
            wrapper.copy(
                    cacheToken = ResponseToken(
                            instruction,
                            NOT_CACHED,
                            requestDate
                    )
            )
        }
    }

    /**
     * Refreshes the Duration metadata
     *
     * @param callDuration the duration of the network call
     * @param diskDuration the duration of the operation to retrieve the response from cache
     * @return the updated Duration metadata
     */
    private fun getRefreshCallDuration(
            callDuration: CallDuration,
            diskDuration: Int
    ) =
            callDuration.copy(
                    disk = diskDuration,
                    network = callDuration.network - diskDuration,
                    total = callDuration.network
            )
}

data class TransientResponse<R : Any>(
        val response: R,
        var cacheToken: RequestToken<Cache, R>
)

