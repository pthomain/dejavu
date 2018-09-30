package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token

enum class CacheStatus constructor(
        isFinal: Boolean,
        /**
         * Single responses are final and are not preceded or succeeded by another response.
         */
        val isSingle: Boolean,
        /**
         * Identifies data that is fresh, either because it comes straight from network
         * or because it is in the cache and hasn't expired yet. This data will be the only
         * type returned in calls made with the freshOnly directive.
         */
        val isFresh: Boolean,
        /**
         * Indicates whether or not the cache metadata contains an error
         * (for responses implementing the CacheMetadata.Holder interface).
         */
        val hasError: Boolean = false
) {
    /**
     * Internal use only, for the request instruction
     */
    INSTRUCTION(false, false, false),

    /**
     * Returned with responses that were not cached, in accordance to the
     * DO_NOT_CACHE instruction explicitly set on the request.
     */
    NOT_CACHED(true, true, true),

    /**
     * Returned with responses coming straight from the network for which no cached data exists.
     */
    FRESH(true, true, true),

    /**
     * Returned with responses coming straight from the cache within their expiry date,
     * no further network call is made (hence isSingle = true).
     */
    CACHED(true, true, true),

    /**
     * Returned with responses coming straight from the cache after their expiry date.
     * This only happens if the all of the following criteria are met:
     *
     * - there is some stale data in the cache for this call
     *
     * - the freshOnly directive is set to false for this call
     *
     * - the data is returned via an Observable and not a Single (in which case only
     * a final response is returned, i.e. either REFRESHED, COULD_NOT_REFRESH or EMPTY).
     */
    STALE(false, false, false),

    /**
     * Returned after a STALE response with FRESH data from a successful network call or
     * alternatively as a single response if the freshOnly directive is set or the response
     * is returned via a Single rather than an Observable.
     */
    REFRESHED(true, false, true),

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
    COULD_NOT_REFRESH(true, false, false, true),

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
     * - the cached data is stale and the freshOnly directive is set to true, which means that there
     * is nothing to deliver. If this is the case, the metadata will hold the cause exception at the
     * same condition that the response implements CacheMetadata.Holder (see above)
     *
     * Metadata on this response will contain an exception, or if the response does not
     * implement CacheMetadata.Holder, the exception will be delivered using the default
     * RxJava error mechanism.
     */
    EMPTY(true, false, true, true);

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