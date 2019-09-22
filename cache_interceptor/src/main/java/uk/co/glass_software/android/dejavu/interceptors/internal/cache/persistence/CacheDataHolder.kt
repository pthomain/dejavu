package uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence

data class CacheDataHolder(
        val hash: String,
        val cacheDate: Long,
        val expiryDate: Long,
        val data: ByteArray,
        val responseClassName: String,
        val isCompressed: Boolean,
        val isEncrypted: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CacheDataHolder

        if (hash != other.hash) return false
        if (cacheDate != other.cacheDate) return false
        if (expiryDate != other.expiryDate) return false
        if (!data.contentEquals(other.data)) return false
        if (responseClassName != other.responseClassName) return false
        if (isCompressed != other.isCompressed) return false
        if (isEncrypted != other.isEncrypted) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + cacheDate.hashCode()
        result = 31 * result + expiryDate.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + responseClassName.hashCode()
        result = 31 * result + isCompressed.hashCode()
        result = 31 * result + isEncrypted.hashCode()
        return result
    }
}