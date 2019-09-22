package uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence

import uk.co.glass_software.android.boilerplate.core.utils.kotlin.ifElse
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus.FRESH
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus.STALE
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import java.util.*

interface PersistenceManager<E>
        where E : Exception,
              E : NetworkErrorProvider {
    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param typeToClear type of entries to clear (or all the entries if this parameter is null)
     * @param clearStaleEntriesOnly only clear STALE entries if set to true (or all otherwise)
     */
    fun clearCache(typeToClear: Class<*>?,
                   clearStaleEntriesOnly: Boolean)

    /**
     * Returns a cached entry if available
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     * @param start the time at which the operation started in order to calculate the time the operation took.
     *
     * @return a cached entry if available, or null otherwise
     */
    fun getCachedResponse(instructionToken: CacheToken,
                          start: Long): ResponseWrapper<E>?

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    fun invalidate(instructionToken: CacheToken): Boolean

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     *
     * @param instruction the INVALIDATE instruction for the desired entry.
     * @param key the key of the entry to invalidate
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    fun checkInvalidation(instruction: CacheInstruction,
                          key: String): Boolean

    /**
     * Caches a given response.
     *
     * @param response the response to cache
     * @param previousCachedResponse the previously cached response if available for the purpose of replicating the previous cache settings for the new entry (i.e. compression and encryption)
     */
    fun cache(response: ResponseWrapper<E>,
              previousCachedResponse: ResponseWrapper<E>?)

    /**
     * Indicates whether or not the entry should be compressed or encrypted based primarily
     * on the settings of the previous cached entry if available. If there was no previous entry,
     * then the cache settings are defined by the operation or, if undefined in the operation,
     * by the values defined globally in CacheConfiguration.
     *
     * @param previousCachedResponse the previously cached response if available for the purpose of replicating the previous cache settings for the new entry (i.e. compression and encryption)
     * @param cacheOperation the cache operation for the entry being saved
     *
     * @return a pair of Boolean indicating in order whether the data was encrypted or compressed
     */
    fun shouldEncryptOrCompress(previousCachedResponse: ResponseWrapper<E>?,
                                cacheOperation: Expiring): Pair<Boolean, Boolean>

    companion object {
        /**
         * Calculates the cache status of a given expiry date.
         *
         * @param expiryDate the date at which the data should expire (become STALE)
         *
         * @return whether the data is FRESH or STALE
         */
        fun getCacheStatus(
                expiryDate: Date,
                dateFactory: (Long?) -> Date
        ) =
                ifElse(
                        dateFactory(null).time >= expiryDate.time,
                        STALE,
                        FRESH
                )
    }
}