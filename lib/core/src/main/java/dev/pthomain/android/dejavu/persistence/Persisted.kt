package dev.pthomain.android.dejavu.persistence

import java.util.*

sealed class Persisted(
        open val requestHash: String,
        open val classHash: String,
        open val requestDate: Date,
        open val expiryDate: Date
) {

    data class Key(
            override val requestHash: String,
            override val classHash: String,
            override val requestDate: Date,
            override val expiryDate: Date,
            val serialisation: String
    ) : Persisted(
            requestHash,
            classHash,
            requestDate,
            expiryDate
    )

    data class Serialised(
            override val requestHash: String,
            override val classHash: String,
            override val requestDate: Date,
            override val expiryDate: Date,
            val serialisation: String,
            val data: ByteArray
    ) : Persisted(
            requestHash,
            classHash,
            requestDate,
            expiryDate
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Serialised

            if (requestHash != other.requestHash) return false
            if (classHash != other.classHash) return false
            if (requestDate != other.requestDate) return false
            if (expiryDate != other.expiryDate) return false
            if (serialisation != other.serialisation) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = requestHash.hashCode()
            result = 31 * result + classHash.hashCode()
            result = 31 * result + requestDate.hashCode()
            result = 31 * result + expiryDate.hashCode()
            result = 31 * result + serialisation.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    data class Deserialised<R : Any>(
            override val requestHash: String,
            override val classHash: String,
            override val requestDate: Date,
            override val expiryDate: Date,
            val serialisation: String,
            val data: R
    ) : Persisted(
            requestHash,
            classHash,
            requestDate,
            expiryDate
    )
}