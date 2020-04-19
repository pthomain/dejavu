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
import dev.pthomain.android.dejavu.DejaVu.Configuration
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.ValidRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.configuration.PersistenceManager
import dev.pthomain.android.dejavu.configuration.PersistenceManager.CacheData
import dev.pthomain.android.dejavu.configuration.PersistenceManager.Companion.getCacheStatus
import dev.pthomain.android.dejavu.configuration.SerialisationException
import dev.pthomain.android.dejavu.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import java.text.SimpleDateFormat
import java.util.*

/**
 * Provides a skeletal implementation of PersistenceManager
 *
 * @param configuration the global cache configuration
 * @param serialisationManager used for the serialisation/deserialisation of the cache entries
 * @param dateFactory class providing the time, for the purpose of testing
 */
abstract class BasePersistenceManager<E>(
        private val configuration: Configuration<E>,
        protected val logger: Logger,
        private val serialisationManager: SerialisationManager<E>,
        protected val dateFactory: (Long?) -> Date
) : PersistenceManager<E>
        where E : Throwable,
              E : NetworkErrorPredicate {

    private val dateFormat = SimpleDateFormat("MMM dd h:m:s", Locale.UK)

    /**
     * Serialises the response and generates all the associated metadata for caching.
     *
     * @param response the response to cache.
     *
     * @return a model containing the serialised data along with the calculated metadata to use for caching it
     */
    protected fun <R : Any> serialise(response: Response<R, Cache>): CacheDataHolder.Complete<R> {
        val instructionToken = response.cacheToken
        val instruction = instructionToken.instruction
        val operation = instruction.operation
        val simpleName = instruction.requestMetadata.responseClass.simpleName

        val metadata = with(operation) {
            SerialisationDecorationMetadata(compress, encrypt)
        }

        logger.d(this, "Caching $simpleName")

        val serialised = serialisationManager.serialise(
                response,
                metadata
        )

        val now = dateFactory(null).time

        return CacheDataHolder.Complete(
                instructionToken.instruction.requestMetadata,
                now,
                now + operation.durationInSeconds * 1000L,
                serialised,
                metadata.isCompressed,
                metadata.isEncrypted
        )
    }

    /**
     * Returns a cached entry if available
     *
     * @param instructionToken the request's instruction token
     *
     * @return a cached entry if available, or null otherwise
     * @throws SerialisationException in case the deserialisation failed
     */
    final override fun <R : Any> getCachedResponse(instructionToken: CacheToken<Cache, R>): CacheData<R>? {
        val requestMetadata = instructionToken.instruction.requestMetadata

        logger.d(this, "Checking for cached ${requestMetadata.responseClass.simpleName}")

        invalidateIfNeeded(
                instructionToken.instruction.operation as? Cache,
                requestMetadata
        )

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
    protected abstract fun <R> getCacheDataHolder(requestMetadata: HashedRequestMetadata<R>): CacheDataHolder?

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
    private fun <R : Any> deserialise(instructionToken: CacheToken<Cache, R>,
                                      cacheDate: Date,
                                      expiryDate: Date,
                                      isCompressed: Boolean,
                                      isEncrypted: Boolean,
                                      localData: ByteArray): CacheData<R>? {
        val requestMetadata = instructionToken.instruction.requestMetadata
        val simpleName = requestMetadata.responseClass.simpleName

        return try {
            serialisationManager.deserialise(
                    instructionToken,
                    localData,
                    //FIXME those values might change for a new call (previous data could have been encrypted but the flags on the new instruction might differ)
                    SerialisationDecorationMetadata(isEncrypted, isCompressed)
            ).let { response ->
                val formattedDate = dateFormat.format(expiryDate)
                logger.d(this, "Returning cached $simpleName cached until $formattedDate")

                val operation = instructionToken.instruction.operation
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
                    requestMetadata.copy(responseClass = Any::class.java as Class<R>) //TODO check
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
    final override fun <R> invalidateIfNeeded(operation: Cache?,
                                              requestMetadata: ValidRequestMetadata<R>) =
            if (operation?.priority?.network?.invalidatesLocalData == true) {
                forceInvalidation(requestMetadata)
            } else false

}
