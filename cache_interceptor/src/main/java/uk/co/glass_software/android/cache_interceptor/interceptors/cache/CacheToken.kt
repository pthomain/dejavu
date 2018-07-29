package uk.co.glass_software.android.cache_interceptor.interceptors.cache

import javolution.util.stripped.FastMap.logger
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheStatus.*
import uk.co.glass_software.android.shared_preferences.utils.Logger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

data class CacheToken constructor(val responseClass: Class<*>,
                                  val instruction: CacheInstruction,
                                  val status: CacheStatus,
                                  val apiUrl: String = "",
                                  val body: String? = null,
                                  val fetchDate: Date? = null,
                                  val cacheDate: Date? = null,
                                  val expiryDate: Date? = null) {

    internal fun getKey(hasher: Hasher): String = getKey(hasher, apiUrl, body)

    companion object {

        internal fun fromInstruction(instruction: CacheInstruction,
                                     apiUrl: String,
                                     body: String?) = CacheToken(
                instruction.responseClass,
                instruction,
                INSTRUCTION,
                apiUrl,
                body
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
                            body: String?): String {
            val urlHash = apiUrl.hashCode()
            return try {
                if (body == null) {
                    hasher.hash(apiUrl)
                } else {
                    hasher.hash("$apiUrl$$body")
                }
            } catch (e: Exception) {
                if (body == null) {
                    urlHash.toString() + ""
                } else {
                    (urlHash * 7 + body.hashCode()).toString()
                }
            }
        }

        internal fun getHasher(logger: Logger): Hasher {
            var messageDigest: MessageDigest?

            try {
                messageDigest = MessageDigest.getInstance("SHA-1")
                logger.d(this, "Using SHA-1 hasher")
            } catch (e: NoSuchAlgorithmException) {
                logger.e(this, "Could not create a SHA-1 message digest")
                messageDigest = null
            }

            if (messageDigest == null) {
                try {
                    messageDigest = MessageDigest.getInstance("MD5")
                    logger.d(this, "Using MD5 hasher")
                } catch (e: NoSuchAlgorithmException) {
                    logger.e(this, "Could not create a MD5 message digest")
                    messageDigest = null
                }
            }

            return Hasher(messageDigest)
        }
    }
}