package uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation

sealed class RequestMetadata(val url: String,
                             val requestBody: String? = null) {

     class UnHashed(url: String,
                   requestBody: String? = null) : RequestMetadata(url, requestBody) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }

        override fun toString(): String {
            return "UnHashed(url='$url', requestBody=$requestBody)"
        }
    }

    class Hashed internal constructor(url: String,
                                      requestBody: String?,
                                      val hash: String) : RequestMetadata(url, requestBody) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Hashed

            if (hash != other.hash) return false

            return true
        }

        override fun hashCode(): Int {
            return hash.hashCode()
        }

        override fun toString(): String {
            return "Hashed(url='$url', requestBody=$requestBody, hash='$hash')"
        }
    }

}