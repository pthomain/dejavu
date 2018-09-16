package uk.co.glass_software.android.cache_interceptor.interceptors.cache

import com.google.gson.Gson
import org.iq80.snappy.Snappy
import uk.co.glass_software.android.boilerplate.log.Logger
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import uk.co.glass_software.android.shared_preferences.StoreEntryFactory

internal class SerialisationManager<E>(private val logger: Logger,
                                       private val storeEntryFactory: StoreEntryFactory,
                                       private val gson: Gson)
        where E : Exception,
              E : (E) -> kotlin.Boolean {

    fun deserialise(responseClass: Class<*>,
                    data: ByteArray,
                    isEncrypted: Boolean,
                    isCompressed: Boolean,
                    onError: () -> Unit): ResponseWrapper<E>? {
        val simpleName = responseClass.simpleName

        try {
            var uncompressed = if (isCompressed)
                Snappy.uncompress(data, 0, data.size).apply {
                    logCompression(data, simpleName, this)
                }
            else data

            val encryptionManager = storeEntryFactory.encryptionManager
            if (isEncrypted && encryptionManager != null) {
                uncompressed = encryptionManager.decryptBytes(uncompressed, DATA_TAG)
            }

            val response = gson.fromJson(String(uncompressed), responseClass)
            return ResponseWrapper(responseClass, response, null)
        } catch (e: Exception) {
            logger.e(e, "Could not deserialise $simpleName: clearing the cache")
            onError()
            return null
        }
    }

    fun serialise(response: Any,
                  encryptData: Boolean,
                  compressData: Boolean): ByteArray? {
        val simpleName = response::class.java.simpleName
        val encryptionManager = storeEntryFactory.encryptionManager

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
