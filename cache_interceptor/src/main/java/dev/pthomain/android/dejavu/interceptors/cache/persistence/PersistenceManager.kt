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

package dev.pthomain.android.dejavu.interceptors.cache.persistence

import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.configuration.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Expiring
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.FRESH
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.STALE
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationException
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import java.util.*

interface PersistenceManager<E>
        where E : Exception,
              E : NetworkErrorPredicate {
    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param typeToClear type of entries to clear (or all the entries if this parameter is null)
     * @param clearStaleEntriesOnly only clear STALE entries if set to true (or all otherwise)
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    fun clearCache(typeToClear: Class<*>?,
                   clearStaleEntriesOnly: Boolean)

    /**
     * Returns a cached entry if available
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     *
     * @return a cached entry if available, or null otherwise
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    fun getCachedResponse(instructionToken: CacheToken): ResponseWrapper<E>?

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    fun invalidate(instructionToken: CacheToken): Boolean

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     *
     * @param instructionToken the INVALIDATE instruction token for the desired entry.
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    fun invalidateIfNeeded(instructionToken: CacheToken): Boolean

    /**
     * Caches a given response.
     *
     * @param responseWrapper the response to cache
     * @param previousCachedResponse the previously cached response if available for the purpose of replicating the previous cache settings for the new entry (i.e. compression and encryption)
     * @throws SerialisationException in case the serialisation failed
     */
    @Throws(SerialisationException::class)
    fun cache(responseWrapper: ResponseWrapper<E>,
              previousCachedResponse: ResponseWrapper<E>?)

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
    fun shouldEncryptOrCompress(previousCachedResponse: ResponseWrapper<E>?,
                                cacheOperation: Expiring): SerialisationDecorationMetadata

    companion object {
        /**
         * Calculates the cache status of a given expiry date.
         *
         * @param expiryDate the date at which the data should expire (become STALE)
         *
         * @return whether the data is FRESH or STALE
         */
        fun ((Long?) -> Date).getCacheStatus(expiryDate: Date) = ifElse(
                this(null).time >= expiryDate.time,
                STALE,
                FRESH
        )
    }
}