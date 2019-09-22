package uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence.file

import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence.BasePersistenceManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import java.io.File
import java.util.*


/**
 * Provides a PersistenceManager implementation saving the responses to the give repository.
 * This would be less performant than the database implementation.
 *
 * Be careful to encrypt the data if you change this directory to a publicly readable directory,
 * see CacheConfiguration.Builder().encryptByDefault().
 *
 * @param context the application context
 * @param cacheDirectory which directory to use to persist the response (use cache dir by default)
 */
class FilePersistenceManager<E> private constructor(
        cacheConfiguration: CacheConfiguration<E>,
        serialisationManager: SerialisationManager<E>,
        dateFactory: (Long?) -> Date,
        private val cacheDirectory: File
) : BasePersistenceManager<E>(
        cacheConfiguration,
        serialisationManager,
        dateFactory
) where E : Exception,
        E : NetworkErrorProvider {

    init {
        cacheDirectory.mkdirs()
    }

    /**
     * Caches a given response.
     *
     * @param response the response to cache
     * @param previousCachedResponse the previously cached response if available for the purpose of replicating the previous cache settings for the new entry (i.e. compression and encryption)
     */
    override fun cache(response: ResponseWrapper<E>,
                       previousCachedResponse: ResponseWrapper<E>?) {


    }

    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param typeToClear type of entries to clear (or all the entries if this parameter is null)
     * @param clearStaleEntriesOnly only clear STALE entries if set to true (or all otherwise)
     */
    override fun clearCache(typeToClear: Class<*>?,
                            clearStaleEntriesOnly: Boolean) {
    }

    /**
     * Returns a cached entry if available
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     * @param start the time at which the operation started in order to calculate the time the operation took.
     *
     * @return a cached entry if available, or null otherwise
     */
    override fun getCachedResponse(instructionToken: CacheToken,
                                   start: Long): ResponseWrapper<E>? {
        return null
    }

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    override fun invalidate(instructionToken: CacheToken): Boolean {
        return false
    }

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     *
     * @param instruction the INVALIDATE instruction for the desired entry.
     * @param key the key of the entry to invalidate
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    override fun checkInvalidation(instruction: CacheInstruction,
                                   key: String): Boolean {
        return false
    }

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
    override fun shouldEncryptOrCompress(previousCachedResponse: ResponseWrapper<E>?,
                                         cacheOperation: Expiring): Pair<Boolean, Boolean> {
        return false to false
    }

    class Factory<E> internal constructor(
            private val cacheConfiguration: CacheConfiguration<E>,
            private val serialisationManager: SerialisationManager<E>,
            private val dateFactory: (Long?) -> Date
    ) where E : Exception,
            E : NetworkErrorProvider {

        fun create(cacheDirectory: File = cacheConfiguration.context.cacheDir) =
                FilePersistenceManager(
                        cacheConfiguration,
                        serialisationManager,
                        dateFactory,
                        cacheDirectory
                )

    }

}