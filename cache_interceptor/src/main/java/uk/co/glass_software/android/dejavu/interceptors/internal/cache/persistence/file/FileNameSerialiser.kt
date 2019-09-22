package uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence.file

import uk.co.glass_software.android.boilerplate.core.utils.kotlin.ifElse
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence.CacheDataHolder
import java.util.*

//TODO test
internal class FileNameSerialiser(
        private val dateFactory: (Long?) -> Date
) {

    fun serialise(cacheDataHolder: CacheDataHolder) =
            with(cacheDataHolder) {
                listOf(
                        hash,
                        expiryDate.toString(),
                        responseClassName,
                        ifElse(isCompressed, "1", "0"),
                        ifElse(isEncrypted, "1", "0")
                ).joinToString(SEPARATOR)
            }

    fun deserialise(fileName: String) =
            with(fileName.split(SEPARATOR)) {
                if (size != 5) null
                else CacheDataHolder(
                        get(0),
                        dateFactory(null).time,
                        get(1).toLong(),
                        ByteArray(0),
                        get(2),
                        get(3) == "1",
                        get(4) == "1"
                )
            }

    companion object {
        const val SEPARATOR = "_-_"
    }
}
