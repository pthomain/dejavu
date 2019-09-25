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

package dev.pthomain.android.dejavu.interceptors.internal.cache.persistence

import dev.pthomain.android.boilerplate.core.utils.lambda.Action.Companion.act
import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Invalidate
import dev.pthomain.android.dejavu.configuration.NetworkErrorProvider
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.token.CacheToken
import dev.pthomain.android.dejavu.response.CacheMetadata
import dev.pthomain.android.dejavu.response.ResponseWrapper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Provides a skeletal implementation of PersistenceManager
 *
 * @param cacheConfiguration the global cache configuration
 */
abstract class BasePersistenceManager<E>(
        protected val cacheConfiguration: CacheConfiguration<E>,
        protected val serialisationManager: SerialisationManager<E>,
        protected val dateFactory: (Long?) -> Date
) : PersistenceManager<E>
        where E : Exception,
              E : NetworkErrorProvider {

    protected val dateFormat = SimpleDateFormat("MMM dd h:m:s", Locale.UK)
    protected val logger = cacheConfiguration.logger
    protected val durationInMillis = cacheConfiguration.cacheDurationInMillis

    /**
     * Indicates whether or not the entry should be compressed or encrypted based primarily
     * on the settings of the previous cached entry if available. If there was no previous entry,
     * then the cache settings are defined by the operation or, if undefined in the operation,
     * by the values defined globally in CacheConfiguration.
     *
     * @param previousCachedResponse the previously cached response if available for the purpose of replicating the previous cache settings for the new entry (i.e. compression and encryption)
     * @param cacheOperation the cache operation for the entry being saved
     *
     * @return a pair of Boolean indicating in order whether the data was encrypted or compressed
     */
    final override fun shouldEncryptOrCompress(previousCachedResponse: ResponseWrapper<E>?,
                                               cacheOperation: Expiring) =
            with(previousCachedResponse?.metadata?.cacheToken) {
                if (this != null)
                    isEncrypted to isCompressed
                else
                    Pair(
                            cacheOperation.encrypt ?: cacheConfiguration.encrypt,
                            cacheOperation.compress ?: cacheConfiguration.compress
                    )
            }

    /**
     * Serialises the response and generates all the associated metadata for caching.
     *
     * @param response the response to cache.
     * @param previousCachedResponse the previously cached response, if available, to determine continued values such as previous encryption etc.
     *
     * @return a model containing the serialised data along with the calculated metadata to use for caching it
     */
    protected fun serialise(response: ResponseWrapper<E>,
                            previousCachedResponse: ResponseWrapper<E>?): CacheDataHolder? {
        val instructionToken = response.metadata.cacheToken
        val instruction = instructionToken.instruction
        val operation = instruction.operation as Expiring
        val simpleName = instruction.responseClass.simpleName
        val durationInMillis = operation.durationInMillis ?: durationInMillis

        logger.d(this, "Caching $simpleName")

        val (encryptData, compressData) = shouldEncryptOrCompress(
                previousCachedResponse,
                operation
        )

        val serialised = serialisationManager.serialise(
                response,
                encryptData,
                compressData
        )

        return if (serialised != null) {
            val now = dateFactory(null).time

            CacheDataHolder(
                    instructionToken.requestMetadata,
                    now,
                    now + durationInMillis,
                    serialised,
                    instructionToken.requestMetadata.classHash,
                    compressData,
                    encryptData
            )
        } else
            null.also {
                logger.e(this, "Could not serialise and store data for $simpleName")
            }
    }

    /**
     * Returns a cached entry if available
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     * @param start the time at which the operation started in order to calculate the time the operation took.
     *
     * @return a cached entry if available, or null otherwise
     */
    final override fun getCachedResponse(instructionToken: CacheToken,
                                         start: Long): ResponseWrapper<E>? {
        val instruction = instructionToken.instruction
        logger.d(this, "Checking for cached ${instruction.responseClass.simpleName}")

        invalidatesIfNeeded(instructionToken)

        val serialised = getCacheDataHolder(
                instructionToken,
                instructionToken.requestMetadata,
                start
        )

        return with(serialised) {
            if (this == null) null
            else deserialise(
                    instructionToken,
                    start,
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
     * @param start the time at which the operation started in order to calculate the time the operation took.
     *
     * @return the cached data as a CacheDataHolder
     */
    protected abstract fun getCacheDataHolder(instructionToken: CacheToken,
                                              requestMetadata: RequestMetadata.Hashed,
                                              start: Long): CacheDataHolder?

    /**
     * Deserialises the cached data
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     * @param start the time at which the operation started in order to calculate the time the operation took.
     * @param cacheDate the Date at which the data was cached
     * @param expiryDate the Date at which the data would become STALE
     * @param isCompressed whether or not the cached data was saved compressed
     * @param isEncrypted whether or not the cached data was saved encrypted
     * @param localData the cached data
     *
     * @return a ResponseWrapper containing the deserialised data or null if the operation failed.
     */
    private fun deserialise(instructionToken: CacheToken,
                            start: Long,
                            cacheDate: Date,
                            expiryDate: Date,
                            isCompressed: Boolean,
                            isEncrypted: Boolean,
                            localData: ByteArray) =
            serialisationManager.deserialise(
                    instructionToken,
                    localData,
                    isEncrypted,
                    isCompressed,
                    { clearCache(null, false) }.act()
            )?.let {
                val callDuration = CacheMetadata.Duration(
                        (dateFactory(null).time - start).toInt(),
                        0,
                        0
                )

                logger.d(
                        this,
                        "Returning cached ${instructionToken.instruction.responseClass.simpleName} cached until ${dateFormat.format(expiryDate)}"
                )
                it.apply {
                    metadata = CacheMetadata(
                            CacheToken.cached(
                                    instructionToken,
                                    PersistenceManager.getCacheStatus(expiryDate, dateFactory),
                                    isCompressed,
                                    isEncrypted,
                                    cacheDate,
                                    expiryDate
                            ),
                            null,
                            callDuration
                    )
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
            invalidatesIfNeeded(
                    instructionToken.copy(
                            instruction = instructionToken.instruction.copy(operation = Invalidate)
                    )
            )
}
