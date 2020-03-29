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

package dev.pthomain.android.dejavu.interceptors.cache.persistence

import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.interceptors.cache.instruction.ValidRequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local.Invalidate
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.FRESH
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.STALE
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationException
import dev.pthomain.android.dejavu.interceptors.response.Response
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import java.util.*

interface PersistenceManager<E>
        where E : Throwable,
              E : NetworkErrorPredicate {
    /**
     * Returns a cached entry if available
     *
     * @param instructionToken the request's instruction token
     *
     * @return a cached entry if available, or null otherwise
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    fun <R : Any> getCachedResponse(instructionToken: CacheToken<Cache, R>): Response<R, Cache>?

    /**
     * Caches a given response.
     *
     * @param responseWrapper the response to cache
     * @throws SerialisationException in case the serialisation failed
     */
    @Throws(SerialisationException::class)
    fun <R : Any> cache(responseWrapper: Response<R, Cache>)

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE).
     *
     * @param operation the request's Invalidate operation
     * @param requestMetadata the request's metadata
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    fun <R> forceInvalidation(operation: Invalidate,
                              requestMetadata: ValidRequestMetadata<R>): Boolean

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     * if the CachePriority requires it.
     *
     * @param operation the request's operation
     * @param requestMetadata the request's metadata
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    fun <R> invalidateIfNeeded(operation: Cache?,
                               requestMetadata: ValidRequestMetadata<R>): Boolean

    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param operation the Clear operation
     * @param requestMetadata the request's metadata
     */
    fun <R> clearCache(operation: Clear,
                       requestMetadata: ValidRequestMetadata<R>)

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