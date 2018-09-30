package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation

import com.google.gson.Gson
import org.iq80.snappy.Snappy
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import uk.co.glass_software.android.shared_preferences.encryption.manager.EncryptionManager

internal class SerialisationManager<E>(private val logger: Logger,
                                       private val encryptionManager: EncryptionManager?,
                                       private val gson: Gson)
        where E : Exception,
              E : NetworkErrorProvider {

    fun deserialise(instructionToken: CacheToken,
                    data: ByteArray,
                    isEncrypted: Boolean,
                    isCompressed: Boolean,
                    onError: () -> Unit): ResponseWrapper<E>? {
        val responseClass = instructionToken.instruction.responseClass
        val simpleName = responseClass.simpleName

        try {
            var uncompressed = if (isCompressed)
                Snappy.uncompress(data, 0, data.size).apply {
                    logCompression(data, simpleName, this)
                }
            else data

            if (isEncrypted && encryptionManager != null) {
                uncompressed = encryptionManager.decryptBytes(uncompressed, DATA_TAG)
            }

            val response = gson.fromJson(String(uncompressed), responseClass)

            return ResponseWrapper(
                    responseClass,
                    response,
                    CacheMetadata<E>(instructionToken, null)
            )
        } catch (e: Exception) {
            logger.e(e, "Could not deserialise $simpleName: clearing the cache")
            onError()
            return null
        }
    }

    fun serialise(responseWrapper: ResponseWrapper<E>,
                  encryptData: Boolean,
                  compressData: Boolean): ByteArray? {
        val simpleName = responseWrapper.responseClass.simpleName
        val response = responseWrapper.response!!

        return gson.toJson(response)
                .toByteArray()
                .let {
                    if (encryptData && encryptionManager != null) encryptionManager.encryptBytes(it, DATA_TAG)
                    else it
                }
                ?.let {
                    if (compressData) Snappy.compress(it).also { compressed ->
                        logCompression(compressed, simpleName, it)
                    }
                    else it
                }
    }

    private fun logCompression(compressedData: ByteArray,
                               simpleName: String,
                               uncompressed: ByteArray) {
        logger.d("Compressed/uncompressed $simpleName: ${compressedData.size}B/${uncompressed.size}B (${100 * compressedData.size / uncompressed.size}%)")
    }

    companion object {
        private const val DATA_TAG = "DATA_TAG"
    }
}
