package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation

import java.io.UnsupportedEncodingException
import java.security.MessageDigest

internal class Hasher(private val messageDigest: MessageDigest?)
    : (ByteArray) -> String {

    @Throws(UnsupportedEncodingException::class)
    fun hash(text: String) = if (messageDigest == null) {
        var hash: Long = 7
        for (i in 0 until text.length) {
            hash = hash * 31 + text[i].toLong()
        }
        hash.toString()
    } else {
        val textBytes = text.toByteArray(charset("UTF-8"))
        messageDigest.update(textBytes, 0, textBytes.size)
        invoke(messageDigest.digest())
    }

    override fun invoke(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    companion object {
        private val hexArray = "0123456789ABCDEF".toCharArray()
    }
}
