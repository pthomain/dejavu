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
/**
 * This class dictates the way the cache should behave in handling a request returning data.
 *
 * @param usesNetwork whether or not the cache should attempt to fetch data from the network
 * @param invalidatesExistingData whether or not cached data should be permanently marked as STALE, regardless of its presence or existing status
 * @param emitsCachedStale whether or not STALE cached data should be returned (prior to a attempting a network call for instance)
 * @param emitsNetworkStale whether or not STALE cached data should be returned after a failed network call (as COULD_NOT_REFRESH)
 * @param hasSingleResponse whether the cache will emit a single response or 2 of them (usually starting with a transient STALE one)
 * @param returnedStatuses the possible statuses of the response(s) emitted by the cache as a result of this priority
 */
enum class CachePriority(
        val usesNetwork: Boolean,
        val invalidatesExistingData: Boolean,
        val emitsCachedStale: Boolean,
        val emitsNetworkStale: Boolean,
        val hasSingleResponse: Boolean,
        vararg returnedStatuses: CacheStatus
) {

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
     *
     * This priority is the most versatile and allows for showing transient STALE data
     * during a loading UI state for instance.
     */
    DEFAULT(
            true,
            false,
            true,
            true,
            false,
            FRESH,
            STALE,
            NETWORK,
            REFRESHED,
            EMPTY,
            COULD_NOT_REFRESH
    ),

    /**
     * Returns cached data only if it is FRESH or if it is STALE attempts to refresh
     * the data by making a network call and by emitting the result of this call even if the call
     * fails (re-emitting the previous STALE response with a COULD_NOT_REFRESH status).
     *
     * No network call is attempted if the cached data is FRESH.
     * STALE data is not emitted from the cache but only if the network call failed (COULD_NOT_REFRESH).
     *
     * Only emits a single response:
     * - FRESH if the cached data is FRESH
     * - NETWORK if there is no cached data and the network call succeeds
     * - REFRESHED if the cached data is STALE and successfully refreshed
     * - COULD_NOT_REFRESH if the cached data is STALE and the network call failed
     * - EMPTY if no cached data was present and the network call failed.
     *
     * This priority ignores cached data if it is STALE but will return it
     * if it could not be refreshed.
     */
    FRESH_PREFERRED(
            true,
            false,
            false,
            true,
            true,
            FRESH,
            NETWORK,
            REFRESHED,
            EMPTY,
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
     * - NETWORK if there is no cached data and the network call succeeds
     * - REFRESHED if the cached data is STALE and successfully refreshed
     * - EMPTY if the cached data is STALE or inexistent and the network call failed.
     *
     * This priority will never return STALE data, either transiently from the cache or
     * as a result of a failed network call.
     */
    FRESH_ONLY(
            true,
            false,
            false,
            false,
            true,
            FRESH,
            NETWORK,
            REFRESHED,
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
     *
     * This priority will first invalidate existing cached data and then behave the same way as
     * DEFAULT. The only difference is that it will never return FRESH cached data since this
     * data is permanently marked as STALE. This STALE cached data will still be returned to
     * be displayed on a loading UI state for instance.
     */
    INVALIDATED(
            true,
            true,
            true,
            true,
            false,
            STALE,
            NETWORK,
            REFRESHED,
            EMPTY,
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
     *
     * This priority will first invalidate existing cached data and then behave the same way as
     * FRESH_ONLY. The only difference is that it will never return FRESH cached data since this
     * data is permanently marked as STALE. No STALE data will ever be returned either initially
     * from the cache or as the result of a failed network call.
     */
    INVALIDATED_FRESH_ONLY(
            true,
            true,
            false,
            false,
            true,
            NETWORK,
            REFRESHED,
            EMPTY
    ),

    /**
     * Returns cached data even if is STALE. No network call is attempted.
     * Returns EMPTY if no cached data is available.
     *
     * Only emits a single response, either:
     * - FRESH
     * - STALE
     * - EMPTY.
     *
     * This priority returns cached data as is without ever using the network.
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
     * Only emits a single response, either:
     * - FRESH
     * - EMPTY if there is no cached data or if it is STALE.
     *
     * This priority returns only FRESH cached data without ever using the network.
     */
    OFFLINE_FRESH_ONLY(
            false,
            false,
            false,
            false,
            true,
            FRESH,
            EMPTY
    )

}