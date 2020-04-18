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

package dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation

import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.STALE_ACCEPTED_FIRST
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Type.*
import dev.pthomain.android.dejavu.configuration.CachePredicate.Companion.DEFAULT_CACHE_DURATION_IN_SECONDS
import dev.pthomain.android.dejavu.utils.swapWhenDefault

/**
 * Represent a cache operation.
 */
sealed class Operation(val type: Type) {

    /**
     * Represents operations returning data, either from the network or from the cache.
     */
    sealed class Remote(type: Type) : Operation(type) {

        /**
         * Expiring instructions contain a durationInMillis indicating the duration of the cached value
         * in milliseconds.
         *
         * This instruction is overridden by the cachePredicate. TODO check this, cache predicate should take precedence
         * @see dev.pthomain.android.dejavu.configuration.DejaVu.Configuration.cachePredicate
         *
         * @param priority the priority instructing how the cache should behave
         * @param durationInSeconds duration of the cache for this specific call in seconds, during which the data is considered FRESH
         * @param connectivityTimeoutInSeconds maximum time to wait for the network connectivity to become available to return an online response (does not apply to cached responses)
         * @param requestTimeOutInSeconds maximum time to wait for the request to finish (does not apply to cached responses)
         * @param encrypt whether the cached data should be encrypted, useful for use on external storage //TODO abstract
         * @param compress whether the cached data should be compressed, useful for large responses //TODO abstract
         */
        class Cache(
                val priority: CachePriority = STALE_ACCEPTED_FIRST,
                durationInSeconds: Int? = DEFAULT_CACHE_DURATION_IN_SECONDS,
                connectivityTimeoutInSeconds: Int? = null,
                requestTimeOutInSeconds: Int? = null,
                val encrypt: Boolean = false,
                val compress: Boolean = false
        ) : Remote(CACHE) {

            val durationInSeconds: Int = durationInSeconds.swapWhenDefault(DEFAULT_CACHE_DURATION_IN_SECONDS)!!
            val connectivityTimeoutInSeconds: Int? = connectivityTimeoutInSeconds.swapWhenDefault(null)
            val requestTimeOutInSeconds: Int? = requestTimeOutInSeconds.swapWhenDefault(null)

            override fun toString() = serialise(
                    priority,
                    durationInSeconds,
                    connectivityTimeoutInSeconds,
                    requestTimeOutInSeconds,
                    encrypt,
                    compress
            )

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Cache

                if (priority != other.priority) return false
                if (encrypt != other.encrypt) return false
                if (compress != other.compress) return false
                if (durationInSeconds != other.durationInSeconds) return false
                if (connectivityTimeoutInSeconds != other.connectivityTimeoutInSeconds) return false
                if (requestTimeOutInSeconds != other.requestTimeOutInSeconds) return false

                return true
            }

            override fun hashCode(): Int {
                var result = priority.hashCode()
                result = 31 * result + encrypt.hashCode()
                result = 31 * result + compress.hashCode()
                result = 31 * result + durationInSeconds
                result = 31 * result + (connectivityTimeoutInSeconds ?: 0)
                result = 31 * result + (requestTimeOutInSeconds ?: 0)
                return result
            }
        }

        /**
         * DO_NOT_CACHE operations will not cache the response.
         */
        object DoNotCache : Remote(DO_NOT_CACHE)
    }

    /**
     * Represents operations operating solely on the local cache and returning no data.
     */
    sealed class Local(type: Type) : Operation(type) {
        /**
         * INVALIDATE instructions invalidate the currently cached data if present and do not return any data.
         * They should usually be used with a Completable. However, if used with a Single or Observable,
         * they will return an empty response with cache metadata (if the response implements CacheMetadata.Holder).
         *
         * This operation will clear entries of the type defined in the associated RequestMetadata.
         * In order to clear all entries, use Any as the response class.
         * @see dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata.responseClass
         */
        object Invalidate : Local(INVALIDATE)

        /**
         * CLEAR instructions clear the cached data for this call if present and do not return any data.
         * They should usually be used with a Completable. However, if used with a Single or Observable,
         * they will return an empty response with cache metadata (if the response implements CacheMetadata.Holder).
         *
         * This operation will clear entries of the type defined in the associated RequestMetadata.
         * In order to clear all entries, use Any as the response class.
         * @see dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata.responseClass
         *
         * @param clearStaleEntriesOnly whether or not to clear the STALE data only. When set to true, only expired data is cleared, otherwise STALE and FRESH data is cleared.
         */
        data class Clear(val clearStaleEntriesOnly: Boolean = false) : Local(CLEAR) {
            override fun toString() = serialise(clearStaleEntriesOnly)
        }
    }

    override fun toString() = serialise()

    enum class Type {
        CACHE,
        DO_NOT_CACHE,
        CLEAR,
        INVALIDATE
    }
}

