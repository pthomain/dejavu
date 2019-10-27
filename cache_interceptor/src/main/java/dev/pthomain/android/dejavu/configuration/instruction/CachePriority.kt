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

import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.*

//TODO use this instead of flags in Operation.Expiring (merge all 3 operations in one)
enum class CachePriority(
        val usesNetwork: Boolean,
        val invalidatesExistingData: Boolean,
        val emitsCachedStale: Boolean,
        val emitsNetworkStale: Boolean,
        val hasSingleResponse: Boolean,
        vararg supportedStatuses: CacheStatus
) {
    /**
     * Returns cached data even if is STALE. No network call is attempted.
     * Returns EMPTY if no cached data is available.
     *
     * Only emits a single response (FRESH, STALE or EMPTY).
     */
    OFFLINE(
            false,
            false,
            true,
            false,
            true,
            FRESH,
            STALE,
            EMPTY
    ),

    /**
     * Returns cached data only if is FRESH. No network call is attempted.
     * Returns EMPTY if no cached data is available.
     *
     * Only emits a single response (FRESH or EMPTY).
     */
    OFFLINE_FRESH_ONLY(
            false,
            false,
            false,
            false,
            true,
            FRESH,
            EMPTY
    ),

    /**
     * Returns cached data even if it is STALE then if it is STALE attempts to refresh
     * the data by making a network call and by emitting the result of this call even if the call
     * failed (re-emitting the previous STALE response with a COULD_NOT_REFRESH status).
     *
     * No network call is attempted if the cached data is FRESH.
     * STALE data can be emitted from the cache and from the network (respectively STALE and COULD_NOT_REFRESH).
     *
     * Emits a single response if:
     * - the cached data is FRESH
     * - there is no cached data (NETWORK if the call succeeds or EMPTY otherwise)
     *
     * Or 2 responses otherwise, STALE first then:
     * - REFRESHED if it is successfully refreshed
     * - COULD_NOT_REFRESH if the network call failed.
     */
    CACHED_OR_NETWORK(
            true,
            false,
            true,
            true,
            false,
            FRESH,
            NETWORK,
            STALE,
            REFRESHED,
            COULD_NOT_REFRESH
    ),

    /**
     * Returns cached data only if it is FRESH or if it is STALE attempts to refresh
     * the data by making a network call and by emitting the result of this call even if the call
     * fails (re-emitting the previous STALE response with a COULD_NOT_REFRESH status).
     *
     * No network call is attempted if the cached data is FRESH.
     * STALE data is emitted only if the network call failed (COULD_NOT_REFRESH).
     *
     * Only emits a single response:
     * - FRESH if the cached data is FRESH
     * - REFRESHED if the cached data is STALE and successfully refreshed
     * - COULD_NOT_REFRESH if the cached data is STALE and the network call failed.
     */
    FRESH_OR_NETWORK(
            true,
            false,
            false,
            true,
            true,
            FRESH,
            REFRESHED,
            COULD_NOT_REFRESH
    ),

    /**
     * Returns cached data only if it is FRESH or if it is STALE attempts to refresh
     * the data by making a network call and by emitting the result of this call only
     * if the call succeeds (or EMPTY otherwise).
     *
     * No network call is attempted if the cached data is FRESH.
     * No STALE data is emitted from the cache or if the network call failed (EMPTY).
     *
     * Only emits a single response:
     * - FRESH if the cached data is FRESH
     * - NETWORK if the cached data is STALE and successfully refreshed
     * - EMPTY if the cached data is STALE and the network call failed.
     */
    FRESH_ONLY(
            true,
            false,
            false,
            false,
            true,
            FRESH,
            EMPTY
    ),

    /**
     * Invalidates the cached data then attempts to refresh
     * the data by making a network call and by emitting the result of this call even if the call
     * fails (re-emitting the previous STALE response with a COULD_NOT_REFRESH status).
     *
     * A network call will always be attempted and the cached data will be invalidated.
     * STALE data can be emitted from the cache and from the network (respectively STALE and COULD_NOT_REFRESH).
     *
     * Emits a single response if:
     * - there is no cached data (NETWORK if the call succeeds or EMPTY otherwise)
     *
     * Or 2 responses otherwise, STALE first then:
     * - REFRESHED if it is successfully refreshed
     * - COULD_NOT_REFRESH if the network call failed.
     */
    INVALIDATED(
            true,
            true,
            true,
            true,
            false,
            NETWORK,
            STALE,
            REFRESHED,
            COULD_NOT_REFRESH
    ),

    /**
     * Invalidates the cached data then attempts to refresh
     * the data by making a network call and by emitting the result of this call only
     * if the call succeeds (or EMPTY otherwise).
     *
     * A network call will always be attempted and the cached data will be invalidated.
     * No STALE data is emitted from the cache or if the network call failed (EMPTY).
     *
     * Only emits a single response:
     * - NETWORK if there was no cached data and the network call succeeds
     * - REFRESHED if there was some invalidated cached data and the network call succeeds
     * - EMPTY if the network call failed.
     */
    INVALIDATED_FRESH_ONLY(
            true,
            true,
            false,
            true,
            true,
            NETWORK,
            REFRESHED,
            EMPTY
    )
}