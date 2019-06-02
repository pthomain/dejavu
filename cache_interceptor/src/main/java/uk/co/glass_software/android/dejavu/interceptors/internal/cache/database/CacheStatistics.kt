package uk.co.glass_software.android.dejavu.interceptors.internal.cache.database

import android.annotation.SuppressLint
import com.jakewharton.fliptables.FlipTable
import uk.co.glass_software.android.boilerplate.core.utils.kotlin.ifElse
import uk.co.glass_software.android.boilerplate.core.utils.log.Logger
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus
import java.text.SimpleDateFormat
import java.util.*

data class CacheStatistics(
        val configuration: CacheConfiguration<*>,
        val entries: List<CacheEntrySummary>
) {
    override fun toString(): String {
        val formattedEntries = when {
            entries.isEmpty() -> FlipTable.of(
                    getCacheEntrySummaryColumnNames(true),
                    arrayOf(arrayOf(
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-"
                    ))
            )

            entries.size == 1 -> entries.first().toString()

            else -> FlipTable.of(
                    getCacheEntrySummaryColumnNames(true),
                    entries.map {
                        it.format(true)
                    }.toTypedArray()
            )
        }

        return "\n" + formattedEntries
    }

    /**
     * Use this method with a logger to prevent the table from being truncated by the max
     * logcat line limit. It outputs each line as a new log.
     *
     * @param logger a Logger instance to output the table
     * @param caller the caller or tag to use for the log
     */
    fun log(logger: Logger,
            caller: Any? = null) {
        toString().split("\n").forEach {
            logger.d(caller ?: this, it)
        }
    }
}

data class CacheEntrySummary(
        val responseClass: Class<*>,
        val fresh: Int,
        val stale: Int,
        val oldestEntry: Date,
        val latestEntry: Date,
        val entries: List<CacheEntry>
) {
    override fun toString(): String {
        return FlipTable.of(
                getCacheEntrySummaryColumnNames(true),
                arrayOf(format(true))
        )
    }

    internal fun format(showClass: Boolean = false): Array<String> {
        val formattedEntries = FlipTable.of(
                getCacheEntryColumnNames(false),
                entries.map {
                    it.format(false)
                }.toTypedArray()
        )

        return arrayOf(
                ifElse(showClass, responseClass.format(), null),
                fresh.toString(),
                stale.toString(),
                dateFormat.format(oldestEntry),
                dateFormat.format(latestEntry),
                formattedEntries
        ).filterNotNull().toTypedArray()
    }

    /**
     * Use this method with a logger to prevent the table from being truncated by the max
     * logcat line limit. It outputs each line as a new log.
     *
     * @param logger a Logger instance to output the table
     * @param caller the caller or tag to use for the log
     */
    fun log(logger: Logger,
            caller: Any? = null) {
        toString().split("\n").forEach {
            logger.d(caller ?: this, it)
        }
    }
}

private fun getCacheEntrySummaryColumnNames(showClass: Boolean = false) =
        arrayOf(
                ifElse(showClass, "Response class", null),
                "Fresh",
                "Stale",
                "Oldest Entry",
                "Latest Entry",
                "Entries"
        ).filterNotNull().toTypedArray()

data class CacheEntry(
        val responseClass: Class<*>,
        val status: CacheStatus,
        val encrypted: Boolean,
        val compressed: Boolean,
        val cacheDate: Date,
        val expiryDate: Date
) {
    override fun toString() =
            FlipTable.of(
                    getCacheEntryColumnNames(true),
                    arrayOf(format(true))
            )

    internal fun format(showClass: Boolean = false) =
            arrayOf(
                    ifElse(showClass, responseClass.format(), null),
                    status.name,
                    ifElse(encrypted, "TRUE", "FALSE"),
                    ifElse(compressed, "TRUE", "FALSE"),
                    dateFormat.format(cacheDate),
                    dateFormat.format(expiryDate)
            ).filterNotNull().toTypedArray()

    /**
     * Use this method with a logger to prevent the table from being truncated by the max
     * logcat line limit. It outputs each line as a new log.
     *
     * @param logger a Logger instance to output the table
     * @param caller the caller or tag to use for the log
     */
    fun log(logger: Logger,
            caller: Any? = null) {
        toString().split("\n").forEach {
            logger.d(caller ?: this, it)
        }
    }
}

private fun getCacheEntryColumnNames(showClass: Boolean = false) =
        arrayOf(
                ifElse(showClass, "Response class", null),
                "Status",
                "Encrypted",
                "Compressed",
                "Cache Date",
                "Expiry Date"
        ).filterNotNull().toTypedArray()

@SuppressLint("SimpleDateFormat")
private val dateFormat = SimpleDateFormat("dd MMM yy 'at' HH:mm:ss.SS")

private fun Class<*>.format() = toString().replace("class ", "")