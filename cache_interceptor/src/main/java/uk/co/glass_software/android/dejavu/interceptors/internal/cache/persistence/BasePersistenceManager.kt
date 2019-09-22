package uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence

import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Provides a skeleton implementation of PersistenceManager
 *
 * @param cacheConfiguration the global cache configuration
 */
abstract class BasePersistenceManager<E>(
        protected val cacheConfiguration: CacheConfiguration<E>,
        protected val serialisationManager: SerialisationManager<E>,
        protected val dateFactory: (Long?) -> Date
) : PersistenceManager<E>
        where E : Exception,
              E : NetworkErrorProvider {

    protected val dateFormat = SimpleDateFormat("MMM dd h:m:s", Locale.UK)
    protected val logger = cacheConfiguration.logger
    protected val durationInMillis = cacheConfiguration.cacheDurationInMillis

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
                                         cacheOperation: Expiring) =
            with(previousCachedResponse?.metadata?.cacheToken) {
                if (this != null)
                    isEncrypted to isCompressed
                else
                    Pair(
                            cacheOperation.encrypt ?: cacheConfiguration.encrypt,
                            cacheOperation.compress ?: cacheConfiguration.compress
                    )
            }

    /**
     * Serialises the response and generates all the associated metadata for caching.
     *
     * @param response the response to cache.
     * @param previousCachedResponse the previously cached response, if available, to determine continued values such as previous encryption etc.
     *
     * @return a model containing the serialised data along with the calculated metadata to use for caching it
     */
    protected fun serialise(response: ResponseWrapper<E>,
                            previousCachedResponse: ResponseWrapper<E>?): Serialised? {
        val instructionToken = response.metadata.cacheToken
        val instruction = instructionToken.instruction
        val operation = instruction.operation as Expiring
        val simpleName = instruction.responseClass.simpleName
        val durationInMillis = operation.durationInMillis ?: durationInMillis

        logger.d(this, "Caching $simpleName")

        val (encryptData, compressData) = shouldEncryptOrCompress(
                previousCachedResponse,
                operation
        )

        val serialised = serialisationManager.serialise(
                response,
                encryptData,
                compressData
        )

        return if (serialised != null) {
            val now = dateFactory(null).time

            Serialised(
                    instructionToken.requestMetadata.hash,
                    now,
                    now + durationInMillis,
                    serialised,
                    instruction.responseClass.name,
                    compressData,
                    encryptData
            )
        } else
            null.also {
                logger.e(this, "Could not serialise and store data for $simpleName")
            }
    }

}
