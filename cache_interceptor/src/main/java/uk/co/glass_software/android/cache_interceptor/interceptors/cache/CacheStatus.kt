package uk.co.glass_software.android.cache_interceptor.interceptors.cache

enum class CacheStatus constructor(
        isFinal: Boolean,
        val isSingle: Boolean //Single responses are final and are not preceded or succeeded by another response.
) {
    //internal use only
    INSTRUCTION(false, false),
    //returned with responses that were not cached
    NOT_CACHED(true, true),
    //returned with responses coming straight from the network
    FRESH(true, true),
    //returned with responses coming straight from the cache within their expiry date
    CACHED(true, true),
    //returned with responses coming straight from the cache after their expiry date
    STALE(false, false),
    //returned after a STALE response with FRESH data from a successful network call
    REFRESHED(true, false),
    //returned after a STALE response with STALE data from an unsuccessful network call
    COULD_NOT_REFRESH(true, false);

    //Final responses will not be succeeded by any other response as part of the same call,
    //while non-final responses will be followed by at least another response.
    val isFinal: Boolean = isSingle || isFinal

}
