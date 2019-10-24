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

package dev.pthomain.android.dejavu.interceptors.cache.persistence.base

import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.configuration.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Expiring
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Invalidate
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManager.Companion.getCacheStatus
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationException
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Provides a skeletal implementation of PersistenceManager
 *
 * @param dejaVuConfiguration the global cache configuration
 * @param serialisationManager used for the serialisation/deserialisation of the cache entries
 * @param dateFactory class providing the time, for the purpose of testing
 */
abstract class BasePersistenceManager<E> internal constructor(private val dejaVuConfiguration: DejaVuConfiguration<E>,
                                                              private val serialisationManager: SerialisationManager<E>,
                                                              protected val dateFactory: (Long?) -> Date)
    : PersistenceManager<E>
        where E : Exception,
              E : NetworkErrorPredicate {

    private val dateFormat = SimpleDateFormat("MMM dd h:m:s", Locale.UK)
    protected val logger = dejaVuConfiguration.logger
    protected val durationInMillis = dejaVuConfiguration.cacheDurationInMillis

    /**
     * Indicates whether or not the entry should be compressed or encrypted based primarily
     * on the settings of the previous cached entry if available. If there was no previous entry,
     * then the cache settings are defined by the operation or, if undefined in the operation,
     * by the values defined globally in DejaVuConfiguration.
     *
     * @param previousCachedResponse the previously cached response if available for the purpose of replicating the previous cache settings for the new entry (i.e. compression and encryption)
     * @param cacheOperation the cache operation for the entry being saved
     *
     * @return a SerialisationDecorationMetadata indicating in order whether the data was encrypted or compressed
     */
    final override fun shouldEncryptOrCompress(previousCachedResponse: ResponseWrapper<E>?,
                                               cacheOperation: Expiring) =
            previousCachedResponse?.metadata?.cacheToken?.let {
                SerialisationDecorationMetadata(
                        it.isCompressed,
                        it.isEncrypted
                )
            } ?: SerialisationDecorationMetadata(
                    cacheOperation.compress ?: dejaVuConfiguration.compress,
                    cacheOperation.encrypt ?: dejaVuConfiguration.encrypt
            )

    /**
     * Serialises the response and generates all the associated metadata for caching.
     *
     * @param response the response to cache.
     * @param previousCachedResponse the previously cached response, if available, to determine continued values such as previous encryption etc.
     *
     * @return a model containing the serialised data along with the calculated metadata to use for caching it
     */
    protected fun serialise(response: ResponseWrapper<E>,
                            previousCachedResponse: ResponseWrapper<E>?): CacheDataHolder.Complete {
        val instructionToken = response.metadata.cacheToken
        val instruction = instructionToken.instruction
        val operation = instruction.operation as Expiring
        val simpleName = instruction.responseClass.simpleName
        val durationInMillis = operation.durationInMillis ?: durationInMillis

        logger.d(this, "Caching $simpleName")

        val metadata = shouldEncryptOrCompress(
                previousCachedResponse,
                operation
        )

        val serialised = serialisationManager.serialise(
                response,
                metadata
        )

        val now = dateFactory(null).time

        return CacheDataHolder.Complete(
                instructionToken.requestMetadata,
                now,
                now + durationInMillis,
                serialised,
                instructionToken.requestMetadata.classHash,
                metadata.isCompressed,
                metadata.isEncrypted
        )
    }

    /**
     * Returns a cached entry if available
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     *
     * @return a cached entry if available, or null otherwise
     */
    final override fun getCachedResponse(instructionToken: CacheToken): ResponseWrapper<E>? {
        val instruction = instructionToken.instruction
        logger.d(this, "Checking for cached ${instruction.responseClass.simpleName}")

        invalidateIfNeeded(instructionToken)

        val serialised = getCacheDataHolder(
                instructionToken,
                instructionToken.requestMetadata
        )

        return with(serialised) {
            if (this == null) null
            else deserialise(
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
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     * @param requestMetadata the associated request metadata
     *
     * @return the cached data as a CacheDataHolder
     */
    protected abstract fun getCacheDataHolder(instructionToken: CacheToken,
                                              requestMetadata: RequestMetadata.Hashed): CacheDataHolder?

    /**
     * Deserialises the cached data
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     * @param cacheDate the Date at which the data was cached
     * @param expiryDate the Date at which the data would become STALE
     * @param isCompressed whether or not the cached data was saved compressed
     * @param isEncrypted whether or not the cached data was saved encrypted
     * @param localData the cached data
     *
     * @return a ResponseWrapper containing the deserialised data or null if the operation failed.
     */
    private fun deserialise(instructionToken: CacheToken,
                            cacheDate: Date,
                            expiryDate: Date,
                            isCompressed: Boolean,
                            isEncrypted: Boolean,
                            localData: ByteArray): ResponseWrapper<E>? {
        val simpleName = instructionToken.instruction.responseClass.simpleName

        return try {
            serialisationManager.deserialise(
                    instructionToken,
                    localData,
                    SerialisationDecorationMetadata(isEncrypted, isCompressed)
            ).let { wrapper ->
                val formattedDate = dateFormat.format(expiryDate)
                logger.d(this, "Returning cached $simpleName cached until $formattedDate")

                wrapper.apply {
                    metadata = CacheMetadata(
                            instructionToken.copy(
                                    status = dateFactory.getCacheStatus(expiryDate),
                                    isCompressed = isCompressed,
                                    isEncrypted = isEncrypted,
                                    fetchDate = cacheDate, //TODO check this
                                    cacheDate = cacheDate,
                                    expiryDate = expiryDate
                            ),
                            null
                    )
                }
            }
        } catch (e: SerialisationException) {
            logger.e(this, "Could not deserialise $simpleName: clearing the cache")
            clearCache(null, false)
            null
        }
    }

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    final override fun invalidate(instructionToken: CacheToken) =
            with(instructionToken) {
                invalidateIfNeeded(
                        copy(instruction = instruction.copy(operation = Invalidate(instructionToken.requestMetadata.responseClass)))
                )
            }

}
