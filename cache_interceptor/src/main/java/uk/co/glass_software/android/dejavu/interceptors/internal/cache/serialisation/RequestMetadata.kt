package uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation

sealed class RequestMetadata(val url: String,
                             val requestBody: String? = null) {

    class UnHashed internal constructor(url: String,
                                        requestBody: String?) : RequestMetadata(url, requestBody){
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }

    class Hashed internal constructor(url: String,
                                      requestBody: String?,
                                      val hash: String) : RequestMetadata(url, requestBody){
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
    }

}