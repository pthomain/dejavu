package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token

import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation.Hasher
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

data class CacheToken internal constructor(val instruction: CacheInstruction,
                                           val status: CacheStatus,
                                           val apiUrl: String = "",
                                           val uniqueParameters: String? = null,
                                           val fetchDate: Date? = null,
                                           val cacheDate: Date? = null,
                                           val expiryDate: Date? = null) {

    internal fun getKey(hasher: Hasher) = getKey(hasher, apiUrl, uniqueParameters)

    companion object {

        internal fun fromInstruction(instruction: CacheInstruction,
                                     apiUrl: String,
                                     uniqueParameters: String?) = CacheToken(
                instruction,
                INSTRUCTION,
                apiUrl,
                uniqueParameters
        )

        internal fun notCached(instructionToken: CacheToken,
                               fetchDate: Date) = instructionToken.copy(
                status = NOT_CACHED,
                fetchDate = fetchDate
        )

        internal fun caching(instructionToken: CacheToken,
                             fetchDate: Date,
                             cacheDate: Date,
                             expiryDate: Date) = instructionToken.copy(
                status = FRESH,
                fetchDate = fetchDate,
                cacheDate = cacheDate,
                expiryDate = expiryDate
        )

        internal fun cached(instructionToken: CacheToken,
                            cacheDate: Date,
                            expiryDate: Date) = instructionToken.copy(
                status = CACHED,
                cacheDate = cacheDate,
                fetchDate = cacheDate,
                expiryDate = expiryDate
        )

        internal fun getKey(hasher: Hasher,
                            apiUrl: String,
                            uniqueParameters: String?) =
                apiUrl.hashCode().let {
                    try {
                        uniqueParameters?.let { hasher.hash("$apiUrl$$uniqueParameters") } ?: hasher.hash(apiUrl)
                    } catch (e: Exception) {
                        if (uniqueParameters == null) it.toString()
                        else (it * 7 + uniqueParameters.hashCode()).toString()
                    }
                }

        internal fun getHasher(logger: Logger): Hasher {
            var messageDigest: MessageDigest?

            try {
                messageDigest = MessageDigest.getInstance("SHA-1")
                logger.d("Using SHA-1 hasher")
            } catch (e: NoSuchAlgorithmException) {
                logger.e("Could not create a SHA-1 message digest")
                messageDigest = null
            }

            if (messageDigest == null) {
                try {
                    messageDigest = MessageDigest.getInstance("MD5")
                    logger.d("Using MD5 hasher")
                } catch (e: NoSuchAlgorithmException) {
                    logger.e("Could not create a MD5 message digest")
                    messageDigest = null
                }
            }

            return Hasher(messageDigest)
        }
    }
}