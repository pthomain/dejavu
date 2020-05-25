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

import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.*
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.Behaviour.*
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.FreshnessPriority.*


/**
 * This class dictates the way the cache should behave in handling a request returning data.
 *
 * For the values with the Behaviour CACHE or REFRESH, there are 3 different ways to deal with
 * STALE cache data:
 *
 * - the default way (preference = DEFAULT) is to always emit STALE data from the cache
 * if available. This data can be displayed as a transient UI state while another call is automatically
 * made to refresh the data. Then a second response is emitted with the network result.
 * Keep in mind that calls using Singles will only ever receive responses with a final status.
 * If your response class implements CacheMetadata.Holder, this status will we returned in the
 * metadata field. It can be used to differentiate STALE data from the FRESH one and for the purpose
 * of filtering for instance.
 * @see CacheStatus.isFinal
 * @see dev.pthomain.android.dejavu.interceptors.cache.metadata.ResponseMetadata.Holder
 *
 * - FRESH preferred (preference = FRESH_PREFERRED): this priority does not
 * emit STALE data from the cache initially. It will instead attempt a network call and return the
 * result of this call. However, if the network call fails, then the STALE cached data is returned
 * with a COULD_NOT_REFRESH status.
 *
 * - FRESH only (preference = FRESH_ONLY): this priority never emits STALE data
 * from the cache. It will instead attempt a network call and return theresult of this call.
 * However, if the network call fails, then an EMPTY response will be emitted.
 *
 * For the priorities with CACHE mode, no network call is made if the cached data is FRESH
 * and this cached data is returned.
 *
 * For the priorities with REFRESH mode, he cached data is always permanently invalidated
 * and considered STALE at the time of the call. This means a network call will always be attempted.
 *
 * @param network the mode in which this priority operates
 * @param freshness the preference regarding the handling of STALE data
 * @param possibleStatuses the possible statuses of the response(s) emitted by the cache as a result of this priority
 */
enum class CachePriority(
        val behaviour: Behaviour,
        val freshness: FreshnessPriority,
        vararg val possibleStatuses: CacheStatus
) {

    /**
     * Returns:
     * - FRESH cached data
     * or
     * - STALE cached data then loads from network.
     *
     * Returns cached data even if it is STALE then if it is STALE attempts to refresh
     * the data by making a network call and by emitting the result of this call even if the call
     * failed (re-emitting the previous STALE response with a COULD_NOT_REFRESH status in case of failure).
     *
     * No network call is attempted if the cached data is FRESH.
     * STALE data can be emitted from the cache and from the network (respectively as STALE and COULD_NOT_REFRESH).
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
    STALE_ACCEPTED_FIRST(
            ONLINE,
            ANY,
            FRESH,
            STALE,
            NETWORK,
            REFRESHED,
            EMPTY,
            COULD_NOT_REFRESH
    ),

    /**
     * Returns STALE cached data as a last resort.
     *
     * Returns cached data only if it is FRESH or, if it is STALE, attempts to refresh
     * the data by making a network call and by emitting the result of this call even if the call
     * fails (re-emitting the previous STALE response with a COULD_NOT_REFRESH status).
     *
     * No network call is attempted if the cached data is FRESH.
     * STALE data is not emitted from the cache but only if the network call failed (COULD_NOT_REFRESH).
     *
     * Only emits a single response:
     * - FRESH if the cached data is FRESH
     * - NETWORK if there is no cached data and the network call succeeded
     * - REFRESHED if the cached data is STALE and the network call succeeded
     * - COULD_NOT_REFRESH if the cached data is STALE and the network call failed
     * - EMPTY if no cached data was present and the network call failed.
     *
     * This priority ignores cached data if it is STALE *but* will return it if the network call fails.
     */
    STALE_ACCEPTED_LAST(
            ONLINE,
            FRESH_PREFERRED,
            FRESH,
            NETWORK,
            REFRESHED,
            EMPTY,
            COULD_NOT_REFRESH
    ),

    /**
     * Never returns STALE.
     *
     * Returns cached data only if it is FRESH or, if it is STALE, attempts to refresh
     * the data by making a network call and by emitting the result of this call only
     * if the call succeeds (or EMPTY otherwise).
     *
     * No network call is attempted if the cached data is FRESH.
     * No STALE data is emitted either from the cache or if the network call failed (returns EMPTY).
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
    STALE_NOT_ACCEPTED(
            ONLINE,
            FRESH_ONLY,
            FRESH,
            NETWORK,
            REFRESHED,
            EMPTY
    ),

    /**
     * Invalidates cached data THEN returns STALE (invalidated) cached data THEN loads from network.
     *
     * Invalidates the cached data then attempts to refresh it by making a network call
     * and by emitting the result of this call even if the call fails (re-emitting the previous
     * STALE response with a COULD_NOT_REFRESH status on network failure).
     *
     * A network call will always be attempted and the cached data will always be invalidated.
     * STALE data can be emitted from the cache and from the network (respectively as STALE and COULD_NOT_REFRESH).
     *
     * Emits a single response *if*:
     * - there is no cached data (with NETWORK status if the call succeeds or EMPTY otherwise)
     *
     * Or 2 responses otherwise, STALE (invalidated) cached data first then:
     * - REFRESHED if the network call succeeds
     * - COULD_NOT_REFRESH if the network call failed.
     *
     * This priority will first invalidate existing cached data and then behave the same way as
     * STALE_ACCEPTED_FIRST. The only difference is that it will never return FRESH cached data since this
     * data is permanently invalidated (marked as STALE). This STALE cached data will still be returned to
     * be displayed on a loading UI state for instance.
     */
    INVALIDATE_STALE_ACCEPTED_FIRST(
            INVALIDATE,
            ANY,
            STALE,
            NETWORK,
            REFRESHED,
            EMPTY,
            COULD_NOT_REFRESH
    ),

    /**
     * Invalidates cache data THEN returns STALE as last resort.
     *
     * Invalidates the cached data then attempts to refresh the data by making a network call
     * and by emitting the result of this call even if the call fails
     * (re-emitting the previous STALE response with a COULD_NOT_REFRESH status).
     *
     * No network call is attempted if the cached data is FRESH.
     * STALE data is not emitted from the cache but only if the network call failed (COULD_NOT_REFRESH).
     *
     * Only emits a single response:
     * - NETWORK if there is no cached data and the network call succeeds
     * - REFRESHED if the cached data is STALE and successfully refreshed
     * - COULD_NOT_REFRESH if the cached data is STALE and the network call failed
     * - EMPTY if no cached data was present and the network call failed.
     *
     * This priority will first invalidate existing cached data and then behave the same way as
     * DEFAULT. The only difference is that it will never return FRESH cached data since this
     * data is permanently marked as STALE. This STALE cached data will still be returned to
     * be displayed on a loading UI state for instance.
     */
    INVALIDATE_STALE_ACCEPTED_LAST(
            INVALIDATE,
            FRESH_PREFERRED,
            NETWORK,
            REFRESHED,
            EMPTY,
            COULD_NOT_REFRESH
    ),

    /**
     * Invalidates THEN never returns STALE.
     *
     * Invalidates the cached data then attempts to refresh the data by making a network call
     * and by emitting the result of this call only if the call succeeds (or EMPTY otherwise).
     *
     * A network call will always be attempted and the cached data will always be invalidated.
     * No STALE data is emitted from the cache or from the network call if it failed (EMPTY).
     *
     * Only emits a single response:
     * - NETWORK if there was no cached data and the network call succeeds
     * - REFRESHED if there was some invalidated cached data and the network call succeeds
     * - EMPTY if the network call failed.
     *
     * This priority will first invalidate existing cached data and then behave the same way as
     * STALE_NOT_ACCEPTED. The only difference is that it will never return FRESH cached data since this
     * data is permanently marked as STALE. No STALE data will ever be returned either initially
     * from the cache or as the result of a failed network call.
     */
    INVALIDATE_STALE_NOT_ACCEPTED(
            INVALIDATE,
            FRESH_ONLY,
            NETWORK,
            REFRESHED,
            EMPTY
    ),

    /**
     * Returns cached data even if it is STALE.
     *
     * No network call is ever attempted.
     * Returns EMPTY if no cached data is available.
     *
     * Only emits a single response, either:
     * - FRESH
     * - STALE
     * - EMPTY.
     *
     * This priority returns cached data as is without ever using the network.
     */
    OFFLINE_STALE_ACCEPTED(
            OFFLINE,
            ANY,
            FRESH,
            STALE,
            EMPTY
    ),

    /**
     * Returns cached data only if it is FRESH.
     *
     * No network call is ever attempted.
     * Returns EMPTY if no cached data is available or if it is STALE.
     *
     * Only emits a single response, either:
     * - FRESH
     * - EMPTY if there is no cached data or if it is STALE.
     *
     * This priority returns only FRESH cached data without ever using the network.
     */
    OFFLINE_STALE_NOT_ACCEPTED(
            OFFLINE,
            FRESH_ONLY,
            FRESH,
            EMPTY
    );

    /**
     * Indicates the mode in which the cache should operate in relation to the handling of existing
     * cache data and as to whether a network call should be attempted (regardless of the device's
     * network availability).
     *
     * - LOCAL_FIRST is the default behaviour and checks the current state of the cached data to determine
     * whether it should be automatically refreshed or not. If the data is still FRESH, it is returned
     * and no call to the network is made. Otherwise, a network is made to refresh it.
     *
     * - INVALIDATE permanently invalidates the local data (if present) and goes straight to network.
     * If the call succeeds, then the network data updates the local one and restores the default
     * expiry behaviour for subsequent calls. This mode is the only one that has a side effect, that
     * is to permanently set the local data to STALE (until successfully refreshed).
     *
     * - LOCAL_ONLY will always return cached data without ever attempting to refresh it. This is
     * completely independent from the device's network availability and is not necessarily
     * the mode that should be used when the device has no connectivity (any of those modes can
     * handle the network being unavailable). LOCAL_ONLY specifically instructs the cache never
     * to call the network even if the data is STALE and the network is available.
     *
     * @param usesNetwork whether or not the cache should attempt to fetch data from the network
     * @param invalidatesLocalData whether or not cached data should be permanently marked as STALE, regardless of its presence or existing status
     */
    enum class Behaviour(val usesNetwork: Boolean) {
        ONLINE(true),
        INVALIDATE(true),
        OFFLINE(false);

        fun isOnline() = this == ONLINE
        fun isInvalidate() = this == INVALIDATE
        fun isOffline() = this == OFFLINE
    }

    /**
     * Indicates how the cache should handle STALE data.
     *
     * - ANY will return STALE cached data initially and then attempt to refresh it (if the
     * CacheMode is not OFFLINE) and emit FRESH network data (if the call succeeds) or re-emit
     * the local STALE data with a COULD_NOT_REFRESH CacheToken status (if the call fails).
     *
     * - FRESH_PREFERRED will never emit the STALE cached data initially but will emit it as the
     * result of a failure of the refresh network call (with the status COULD_NOT_REFRESH).
     *
     * - FRESH_ONLY will never emit any STALE data, even if the network call fails. Instead, it
     * will emit an EMPTY response. Beware that responses with an EMPTY status are emitted as
     * exceptions unless you have declared your call to return DejaVuResult<R>, with R being the
     * type of your response, in which case this wrapper will contain this status in the metadata
     * field.
     * @see dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
     *
     * @param emitsCachedStale whether or not STALE cached data should be returned (prior to a attempting a network call for instance)
     * @param emitsNetworkStale whether or not STALE cached data should be returned after a failed network call (as COULD_NOT_REFRESH)
     * @param hasSingleResponse whether the cache will emit a single response or 2 of them (usually starting with a transient STALE one)
     */
    enum class FreshnessPriority(
            val emitsCachedStale: Boolean,
            val emitsNetworkStale: Boolean,
            val hasSingleResponse: Boolean
    ) {
        ANY(true, true, false),
        FRESH_PREFERRED(false, true, true),
        FRESH_ONLY(false, false, true);

        fun isAny() = this == ANY
        fun isFreshPreferred() = this == FRESH_PREFERRED
        fun isFreshOnly() = this == FRESH_ONLY
    }

    companion object {

        /**
         * Commodity factory
         *
         * @param behaviour the desired network priority
         * @param freshness the desired freshness priority
         * @return the corresponding cache priority
         */
        fun with(
                behaviour: Behaviour,
                freshness: FreshnessPriority
        ) =
                when (behaviour) {
                    ONLINE -> when (freshness) {
                        ANY -> STALE_ACCEPTED_FIRST
                        FRESH_PREFERRED -> STALE_ACCEPTED_LAST
                        FRESH_ONLY -> STALE_NOT_ACCEPTED
                    }

                    INVALIDATE -> when (freshness) {
                        ANY -> INVALIDATE_STALE_ACCEPTED_FIRST
                        FRESH_PREFERRED -> INVALIDATE_STALE_ACCEPTED_LAST
                        FRESH_ONLY -> INVALIDATE_STALE_NOT_ACCEPTED
                    }

                    OFFLINE -> when (freshness) {
                        FRESH_ONLY -> OFFLINE_STALE_NOT_ACCEPTED
                        else -> OFFLINE_STALE_ACCEPTED
                    }
                }
    }
}

