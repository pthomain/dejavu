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

package dev.pthomain.android.dejavu.interceptors.cache.instruction

import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration.Companion.DEFAULT_CACHE_DURATION_IN_SECONDS
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority.*
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority.CacheMode.OFFLINE
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority.CacheMode.REFRESH
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority.CachePreference.FRESH_ONLY
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority.CachePreference.FRESH_PREFERRED
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation.Type.*

/**
 * Represent a cache operation. Directives defined here take precedence over global config.
 *
 * @param type the operation type
 */
sealed class Operation(val type: Type) {

    /**
     * Expiring instructions contain a durationInMillis indicating the duration of the cached value
     * in milliseconds.
     *
     * This instruction is overridden by the cachePredicate. TODO check this, cache predicate should take precedence
     * @see dev.pthomain.android.dejavu.configuration.DejaVuConfiguration.cachePredicate
     *
     * @param priority the priority instructing how the cache should behave
     * @param durationInSeconds duration of the cache for this specific call in seconds, during which the data is considered FRESH
     * @param connectivityTimeoutInSeconds maximum time to wait for the network connectivity to become available to return an online response (does not apply to cached responses)
     * @param requestTimeOutInSeconds maximum time to wait for the request to finish (does not apply to cached responses)
     * @param encrypt whether the cached data should be encrypted, useful for use on external storage //TODO abstract
     * @param compress whether the cached data should be compressed, useful for large responses //TODO abstract
     */
    data class Cache(val priority: CachePriority = DEFAULT,
                     val durationInSeconds: Int = DEFAULT_CACHE_DURATION_IN_SECONDS,
                     val connectivityTimeoutInSeconds: Int? = null,
                     val requestTimeOutInSeconds: Int? = null,
                     val encrypt: Boolean = false,
                     val compress: Boolean = false) : Operation(CACHE) {
        override fun toString() = SERIALISER.serialise(
                type,
                priority,
                durationInSeconds,
                connectivityTimeoutInSeconds,
                requestTimeOutInSeconds,
                encrypt,
                compress
        )

        fun isCache() = priority.mode == CacheMode.CACHE
        fun isRefresh() = priority.mode == REFRESH
        fun isOffline() = priority.mode == OFFLINE
        fun isDefault() = priority.preference == CachePreference.DEFAULT
        fun isFreshOnly() = priority.preference == FRESH_ONLY
        fun isFreshPreferred() = priority.preference == FRESH_PREFERRED
    }

    /**
     * DO_NOT_CACHE instructions are not attempting to cache the response. However, generic error handling
     * will still be applied.
     *
     * @see dev.pthomain.android.dejavu.configuration.error.ErrorFactory
     */
    object DoNotCache : Operation(DO_NOT_CACHE)

    /**
     * INVALIDATE instructions invalidate the currently cached data if present and do not return any data.
     * They should usually be used with a Completable. However, if used with a Single or Observable,
     * they will return an empty response with cache metadata (if the response implements CacheMetadata.Holder).
     *
     * This operation will clear entries of the type defined in the associated RequestMetadata.
     * In order to clear all entries, use Any as the response class.
     * @see dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata.responseClass
     * @see DejaVuCall
     *
     * @param useRequestParameters whether or not the request parameters should be used to identify the unique cached entry to invalidate
     */
    data class Invalidate(val useRequestParameters: Boolean = false) : Operation(INVALIDATE) {
        override fun toString() = SERIALISER.serialise(
                type,
                useRequestParameters
        )
    }

    /**
     * CLEAR instructions clear the cached data for this call if present and do not return any data.
     * They should usually be used with a Completable. However, if used with a Single or Observable,
     * they will return an empty response with cache metadata (if the response implements CacheMetadata.Holder).
     *
     * This operation will clear entries of the type defined in the associated RequestMetadata.
     * In order to clear all entries, use Any as the response class.
     * @see dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata.responseClass
     * @see DejaVuCall
     *
     * @param useRequestParameters whether or not the request parameters should be used to identify the unique cached entry to clear
     * @param clearStaleEntriesOnly whether or not to clear the STALE data only. When set to true, only expired data is cleared, otherwise STALE and FRESH data is cleared.
     */
    data class Clear(val useRequestParameters: Boolean = false,
                     val clearStaleEntriesOnly: Boolean = false) : Operation(CLEAR) {
        override fun toString() = SERIALISER.serialise(
                type,
                useRequestParameters,
                clearStaleEntriesOnly
        )
    }

    override fun toString() = SERIALISER.serialise(type)

    companion object {
        @JvmStatic
        private val SERIALISER = OperationSerialiser()

        fun fromString(string: String) = SERIALISER.deserialise(string)
    }

    /**
     * The operation's type.
     *
     * @param isCacheOperation whether or not this operation returns data and as such can be only used with a CacheOperation.
     * @see DejaVuCall
     */
    enum class Type(val isCacheOperation: Boolean = false) {
        CACHE,
        DO_NOT_CACHE,
        INVALIDATE(true),
        CLEAR(true)
    }

}
