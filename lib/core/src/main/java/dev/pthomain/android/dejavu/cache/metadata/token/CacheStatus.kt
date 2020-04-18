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

package dev.pthomain.android.dejavu.cache.metadata.token

/**
 * TODO update JavaDoc
 * Class representing the cache status of a response
 */
enum class CacheStatus(
        /**
         * Whether or not the status is final, meaning that no subsequent response will be emitted.
         * STALE is the only non-final status, which means another (final) response will be emitted
         * after it. In the case of RxJava Singles, a single element will be emitted, which means only
         * responses with a final status can be emitted by Singles unless the global
         * allowNonFinalForSingle directive is set to true and filterFinal is set to false on
         * the call's directives.
         *
         * //TODO update this
         * @see dev.pthomain.android.dejavu.configuration.DejaVu.Configuration.allowNonFinalForSingle
         * @see dev.pthomain.android.dejavu.configuration.Operation.Expiring.filterFinal
         */
        isFinal: Boolean,
        /**
         * Single responses are final and are not preceded or succeeded by any other response.
         *
         * @see isFinal
         */
        val isSingle: Boolean,
        /**
         * Identifies data that is fresh, either because it comes straight from network
         * or because it is in the cache and hasn't expired yet. This data will be the only
         * type returned in calls made with the freshOnly directive.
         *
         * @see dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Expiring.freshOnly
         */
        val isFresh: Boolean,
        /**
         * Identifies data that is from the local cache.
         */
        val isFromCache: Boolean,
        /**
         * Indicates whether or not this status represents an error and as such contains an
         * error in the cache metadata (for responses implementing the CacheMetadata.Holder interface).
         * Responses with this status may or may not have a response, but they'll always have an exception.
         */
        val isError: Boolean = false
) {
    /**
     * Internal use only, for the request instruction
     */
    INSTRUCTION(false, false, false, false),

    /**
     * Returned with responses that were not cached, in accordance to the
     * DO_NOT_CACHE instruction explicitly set on the request.
     */
    NOT_CACHED(true, true, true, false),

    /**
     * Returned with responses coming straight from the network for which no cached data exists.
     */
    NETWORK(true, true, true, false),

    /**
     * Returned with responses coming straight from the cache within their expiry date,
     * no further network call is made (hence isSingle = true).
     */
    FRESH(true, true, true, true),

    /**
     * Returned with responses coming straight from the cache after their expiry date.
     * This only happens if the all of the following criteria are met:
     *
     *
     * - the freshOnly directive is set to false for this call
     *
     * - the data is returned via an Observable and not a Single (in which case only
     * a final response is returned, i.e. either REFRESHED, COULD_NOT_REFRESH or EMPTY).
     */
    STALE(false, false, false, true),

    /**
     * Returned with responses coming straight from the cache after their expiry date.
     * This only happens if the all of the following criteria are met:
     *
     * - there is some stale data in the cache for this call
     *
     * - the network priority is LOCAL_ONLY
     *
     *  This status differs from STALE in that it is final.
     */
    OFFLINE_STALE(true, false, false, true),

    /**
     * Returned after a STALE response with NETWORK data from a successful network call or
     * alternatively as a single response if the freshOnly directive is set or the response
     * is returned via a Single rather than an Observable.
     */
    REFRESHED(true, false, true, false),

    /**
     * Returned after a STALE response with STALE data from an unsuccessful network call or
     * alternatively as a single response if the response is returned via a Single rather
     * than an Observable.
     * This would only happen if the freshOnly directive is set to false for this call,
     * otherwise an EMPTY response is returned.
     *
     * Metadata on this response will contain an exception, or if the response does not
     * implement CacheMetadata.Holder, the exception will be delivered using the default
     * RxJava error mechanism.
     */
    COULD_NOT_REFRESH(true, false, false, true, true),

    /**
     * Returned when no data is available as a result of a network error and either:
     *
     * - no cached data is available and the mergeOnNextOnError is set to true,
     * meaning that an empty response is returned with the exception added to the metadata
     * (if possible: /!\ mergeOnNextOnError should only be used for calls returning a response
     * implementing the CacheMetadata.Holder interface. Failing this, the metadata can't be set
     * and the exception is delivered via the default RxJava error mechanism, potentially causing
     * a crash if the method was not implemented).
     *
     * OR
     *
     * - the cached data is STALE and the freshOnly directive is set to true, which means that there
     * is nothing to deliver. If this is the case, the metadata will hold the cause exception at the
     * same condition that the response implements CacheMetadata.Holder (see above)
     *
     * Metadata on this response will contain an exception, or if the response does not
     * implement CacheMetadata.Holder, the exception will be delivered using the default
     * RxJava error mechanism.
     */
    EMPTY(true, false, true, false, true),

    /**
     * Returned for instructions that do not return any data (e.g. INVALIDATE or CLEAR).
     * This status indicates that the instruction was properly processed.
     */
    DONE(true, true, true, false, false);

    /**
     * This field indicates a response that won't be succeeded by another one.
     *
     * Non-final responses (like STALE) are only emitted with an Observable if certain
     * conditions are met.
     *
     * A Single will only ever emit final responses.
     *
     * Non-final responses will be followed by at least one more response as part of the
     * same call.
     */
    val isFinal: Boolean = isSingle || isFinal

}
