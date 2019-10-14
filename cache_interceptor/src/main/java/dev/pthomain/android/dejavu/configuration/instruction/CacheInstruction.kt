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

package dev.pthomain.android.dejavu.configuration.instruction

import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Type.*

/**
 * Contains the cache operation, target response class and call-specific directives.
 * Those directives take precedence over the ones defined in the global configuration, if applicable.
 *
 * @param responseClass the target response class
 * @param operation the cache operation with call-specific directives
 */
data class CacheInstruction constructor(val responseClass: Class<*>,
                                        val operation: Operation) {

    /**
     * Represent a cache operation. Directives defined here take precedence over global config.
     */
    sealed class Operation(val type: Type) {

        /**
         * Expiring instructions contain a durationInMillis indicating the duration of the cached value
         * in milliseconds.
         *
         * @param durationInMillis duration of the cache for this specific call in milliseconds, during which the data is considered FRESH
         * @param connectivityTimeoutInMillis maximum time to wait for the network connectivity to become available to return an online response (does not apply to cached responses)
         * @param freshOnly whether or not the operation allows STALE data to be returned from the cache
         * @param mergeOnNextOnError allows exceptions to be intercepted and treated as an empty response metadata and delivered as such via onNext. Only used if the the response implements CacheMetadata.Holder. An exception is thrown otherwise.
         * @param encrypt whether the cached data should be encrypted, useful for use on external storage
         * @param compress whether the cached data should be compressed, useful for large responses
         * @param filterFinal whether this operation should return data in a transient state (i.e. STALE and awaiting refresh). Singles will always return final data unless the global allowNonFinalForSingle directive is set to true.
         * @param type the operation type
         */
        sealed class Expiring(val durationInMillis: Long?,
                              val connectivityTimeoutInMillis: Long?,
                              val freshOnly: Boolean,
                              val mergeOnNextOnError: Boolean?,
                              val encrypt: Boolean?,
                              val compress: Boolean?,
                              val filterFinal: Boolean,
                              type: Type) : Operation(type) {

            /**
             * CACHE instructions are the default ones. They will fetch data from the network if no FRESH data
             * is found in the cache. Otherwise, cache data is returned and no network call is attempted.
             * If no FRESH data is found locally a network call is made and the result is returned then cached
             * for the duration defined by durationInMillis.
             *
             * This instruction takes precedence over the global cacheAllByDefault directive.
             * @see DejaVuConfiguration.cacheAllByDefault
             *
             * @param durationInMillis duration of the cache for this specific call in milliseconds, during which the data is considered FRESH
             * @param connectivityTimeoutInMillis maximum time to wait for the network connectivity to become available to return an online response (does not apply to cached responses)
             * @param freshOnly whether or not the operation allows STALE data to be returned from the cache
             * @param mergeOnNextOnError allows exceptions to be intercepted and treated as an empty response metadata and delivered as such via onNext. Only used if the the response implements CacheMetadata.Holder. An exception is thrown otherwise.
             * @param encrypt whether the cached data should be encrypted, useful for use on external storage
             * @param compress whether the cached data should be compressed, useful for large responses
             * @param filterFinal whether this operation should return data in a transient state (i.e. STALE and awaiting refresh). Singles will always return final data unless the global allowNonFinalForSingle directive is set to true.
             */
            class Cache(durationInMillis: Long? = null,
                        connectivityTimeoutInMillis: Long? = null,
                        freshOnly: Boolean = false,
                        mergeOnNextOnError: Boolean? = null,
                        encrypt: Boolean? = null,
                        compress: Boolean? = null,
                        filterFinal: Boolean = false)
                : Expiring(
                    durationInMillis,
                    connectivityTimeoutInMillis,
                    freshOnly,
                    mergeOnNextOnError,
                    encrypt,
                    compress,
                    filterFinal,
                    CACHE
            )

            /**
             * REFRESH instructions will invalidate the data currently cached for the call
             * and force a refresh even though the data might still be considered FRESH.
             * Once invalidated through a REFRESH call, the data is considered permanently STALE
             * until REFRESHED. This is the equivalent of chaining INVALIDATE and CACHE.
             *
             * @param durationInMillis duration of the cache for this specific call in milliseconds, during which the data is considered FRESH
             * @param connectivityTimeoutInMillis maximum time to wait for the network connectivity to become available to return an online response (does not apply to cached responses)
             * @param freshOnly whether or not the operation allows STALE data to be returned from the cache
             * @param mergeOnNextOnError allows exceptions to be intercepted and treated as an empty response metadata and delivered as such via onNext. Only used if the the response implements CacheMetadata.Holder. An exception is thrown otherwise.
             * @param filterFinal whether this operation should return data in a transient state (i.e. STALE and awaiting refresh). Singles will always return final data unless the global allowNonFinalForSingle directive is set to true.
             *
             * @see Invalidate
             * */
            class Refresh(durationInMillis: Long? = null,
                          connectivityTimeoutInMillis: Long? = null,
                          freshOnly: Boolean = false,
                          mergeOnNextOnError: Boolean? = null,
                          filterFinal: Boolean = false)
                : Expiring(
                    durationInMillis,
                    connectivityTimeoutInMillis,
                    freshOnly,
                    mergeOnNextOnError,
                    null,
                    null,
                    filterFinal,
                    REFRESH
            )

            /**
             * OFFLINE instructions will only return cached data if available or an empty response
             * if none is available. This call can return either FRESH or STALE data (if the freshOnly directive is not set).
             * No network call will ever be attempted.
             *
             * @param freshOnly whether or not the operation allows STALE data to be returned from the cache
             * @param mergeOnNextOnError allows exceptions to be intercepted and treated as an empty response metadata and delivered as such via onNext. Only used if the the response implements CacheMetadata.Holder. An exception is thrown otherwise.
             */
            class Offline(freshOnly: Boolean = false,
                          mergeOnNextOnError: Boolean? = null)
                : Expiring(
                    null,
                    null,
                    freshOnly,
                    mergeOnNextOnError,
                    null,
                    null,
                    false,
                    OFFLINE
            )

            override fun toString() = serialiser.serialise(
                    type,
                    durationInMillis,
                    connectivityTimeoutInMillis,
                    freshOnly,
                    mergeOnNextOnError,
                    encrypt,
                    compress,
                    filterFinal
            )

        }

        /**
         * DO_NOT_CACHE instructions are not attempting to cache the response. However, generic error handling
         * will still be applied.
         *
         * @see dev.pthomain.android.dejavu.configuration.ErrorFactory
         */
        object DoNotCache : Operation(DO_NOT_CACHE)

        /**
         * INVALIDATE instructions invalidate the currently cached data if present and do not return any data.
         * They should usually be used with a Completable. However, if used with a Single or Observable,
         * they will return an empty response with cache metadata (if the response implements CacheMetadata.Holder).
         */
        object Invalidate : Operation(INVALIDATE)

        /**
         * CLEAR instructions clear the cached data for this call if present and do not return any data.
         * They should usually be used with a Completable. However, if used with a Single or Observable,
         * they will return an empty response with cache metadata (if the response implements CacheMetadata.Holder).
         *
         * @param typeToClear the response type to clear. When provided, only cached responses of the given type will be cleared. If left null, the entire cache will be cleared.
         * @param clearStaleEntriesOnly whether or not to clear the STALE data only. When set to true, only expired data is cleared, otherwise STALE and FRESH data is cleared.
         */
        data class Clear(val typeToClear: Class<*>? = null,
                         val clearStaleEntriesOnly: Boolean = false) : Operation(CLEAR) {

            override fun toString() = serialiser.serialise(
                    type,
                    typeToClear,
                    clearStaleEntriesOnly
            )
        }

        override fun toString() = serialiser.serialise(type)

        /**
         * The operation's type.
         *
         * @param annotationName the associated annotation name.
         * @param isCompletable whether or not this operation returns data and as such can be used with a Completable.
         */
        enum class Type(val annotationName: String,
                        val isCompletable: Boolean = false) {
            DO_NOT_CACHE("@DoNotCache"),
            CACHE("@Cache"),
            REFRESH("@Refresh"),
            OFFLINE("@Offline"),
            INVALIDATE("@Invalidate", true),
            CLEAR("@Clear", true)
        }

    }

    override fun toString() = serialiser.serialise(
            null,
            responseClass,
            operation
    )

    override fun equals(other: Any?) =
            other is CacheInstruction && other.toString() == toString()

    override fun hashCode() =
            toString().hashCode()

}

private val serialiser: CacheInstructionSerialiser = CacheInstructionSerialiser()
