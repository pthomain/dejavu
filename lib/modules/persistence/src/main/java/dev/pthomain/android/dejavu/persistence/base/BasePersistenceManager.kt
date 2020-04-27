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

package dev.pthomain.android.dejavu.persistence.base

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.persistence.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.shared.PersistenceManager
import dev.pthomain.android.dejavu.shared.PersistenceManager.CacheData
import dev.pthomain.android.dejavu.shared.serialisation.SerialisationException
import dev.pthomain.android.dejavu.shared.token.CacheToken
import dev.pthomain.android.dejavu.shared.token.getCacheStatus
import dev.pthomain.android.dejavu.shared.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.shared.token.instruction.ValidRequestMetadata
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Remote.Cache
import java.text.SimpleDateFormat
import java.util.*

/**
 * Provides a skeletal implementation of PersistenceManager
 *
 * @param serialisationManager used for the serialisation/deserialisation of the cache entries
 * @param dateFactory class providing the time, for the purpose of testing
 */
abstract class BasePersistenceManager(
        protected val logger: Logger,
        private val serialisationManager: SerialisationManager,
        protected val dateFactory: (Long?) -> Date
) : PersistenceManager {

    private val dateFormat = SimpleDateFormat("MMM dd h:m:s", Locale.UK)

    /**
     * Serialises the response and generates all the associated metadata for caching.
     *
     * @param response the response to cache.
     *
     * @return a model containing the serialised data along with the calculated metadata to use for caching it
     */
    protected fun <R : Any> serialise(
            response: R,
            instructionToken: CacheToken<Cache, R>
    ): CacheDataHolder.Complete<R> {
        val instruction = instructionToken.instruction
        val simpleName = response::class.java.simpleName
        logger.d(this, "Caching $simpleName")

        val serialised = serialisationManager.serialise(
                response,
                instruction.operation,
                decorator
        )

        val now = dateFactory(null).time

        with(instruction) {
            return CacheDataHolder.Complete(
                    requestMetadata,
                    now,
                    now + operation.durationInSeconds * 1000L,
                    serialised,
                    instruction.operation.compress,
                    instruction.operation.encrypt
            )
        }
    }

    /**
     * Returns a cached entry if available
     *
     * @param instructionToken the request's instruction token
     *
     * @return a cached entry if available, or null otherwise
     * @throws SerialisationException in case the deserialisation failed
     */
    final override fun <R : Any> find(instructionToken: CacheToken<Cache, R>): CacheData<R>? {
        val requestMetadata = instructionToken.instruction.requestMetadata

        logger.d(this, "Checking for cached ${requestMetadata.responseClass.simpleName}")

        invalidateIfNeeded(instructionToken)

        val serialised = getCacheDataHolder(requestMetadata)

        return serialised?.run {
            deserialise(
                    instructionToken,
                    dateFactory(cacheDate),
                    dateFactory(expiryDate),
                    isCompressed,
                    isEncrypted,
                    data
            )
        }
    }

    /**
     * Returns the cached data as a CacheDataHolder object.
     *
     * @param requestMetadata the associated request metadata
     *
     * @return the cached data as a CacheDataHolder
     */
    protected abstract fun <R : Any> getCacheDataHolder(requestMetadata: HashedRequestMetadata<R>): CacheDataHolder?

    /**
     * Deserialises the cached data
     *
     * @param instructionToken the request's instruction token
     * @param cacheDate the Date at which the data was cached
     * @param expiryDate the Date at which the data would become STALE
     * @param isCompressed whether or not the cached data was saved compressed
     * @param isEncrypted whether or not the cached data was saved encrypted
     * @param localData the cached data
     *
     * @return a ResponseWrapper containing the deserialised data or null if the operation failed.
     */
    private fun <R : Any> deserialise(
            instructionToken: CacheToken<Cache, R>,
            cacheDate: Date,
            expiryDate: Date,
            isCompressed: Boolean,
            isEncrypted: Boolean,
            localData: ByteArray
    ): CacheData<R>? {
        val instruction = instructionToken.instruction
        val requestMetadata = instruction.requestMetadata
        val simpleName = requestMetadata.responseClass.simpleName
        val operation = with(instruction.operation) {
            Cache(
                    priority,
                    durationInSeconds,
                    connectivityTimeoutInSeconds,
                    requestTimeOutInSeconds,
                    isEncrypted,
                    isCompressed
            )
        }

        return try {
            serialisationManager.deserialise(
                    instruction.requestMetadata.responseClass,
                    operation,
                    localData,
                    decorator
            ).let { response ->
                val formattedDate = dateFormat.format(expiryDate)
                logger.d(this, "Returning cached $simpleName cached until $formattedDate")

                val status = dateFactory.getCacheStatus(
                        expiryDate,
                        operation
                )

                logger.d(
                        this,
                        "Found cached $simpleName, status: $status"
                )

                CacheData(
                        response,
                        requestDate = cacheDate, //TODO check this
                        cacheDate = cacheDate,
                        expiryDate = expiryDate
                )
            }
        } catch (e: SerialisationException) {
            logger.e(this, "Could not deserialise $simpleName: clearing the cache")
            clearCache(
                    Clear(false),
                    with(requestMetadata) {
                        ValidRequestMetadata(
                                Any::class.java,
                                url,
                                requestBody,
                                requestHash,
                                classHash
                        )
                    }
            )
            null
        }
    }

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     * only if the CachePriority requires it.
     *
     * @param operation the request's operation
     * @param requestMetadata the request's metadata
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    final override fun <R : Any> invalidateIfNeeded(instructionToken: CacheToken<*, R>) =
            (instructionToken.instruction.operation as? Cache)?.run {
                if (priority.network.invalidatesLocalData) {
                    forceInvalidation(instructionToken.instruction.requestMetadata)
                } else false
            } ?: false

}
