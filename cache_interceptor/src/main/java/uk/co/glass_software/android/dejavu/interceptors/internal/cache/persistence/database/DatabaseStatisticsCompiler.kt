package uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence.database

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import io.reactivex.Single
import uk.co.glass_software.android.boilerplate.core.utils.io.useAndLogError
import uk.co.glass_software.android.boilerplate.core.utils.log.Logger
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence.PersistenceManager.Companion.getCacheStatus
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence.database.SqlOpenHelperCallback.Companion.COLUMNS.*
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence.database.SqlOpenHelperCallback.Companion.TABLE_CACHE
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus.FRESH
import java.util.*

//TODO JavaDoc
internal class DatabaseStatisticsCompiler(
        private val configuration: CacheConfiguration<*>,
        private val logger: Logger,
        private val dateFactory: (Long?) -> Date,
        private val database: SupportSQLiteDatabase
) {

    private val projection = arrayOf(
            CLASS.columnName,
            IS_ENCRYPTED.columnName,
            IS_COMPRESSED.columnName,
            DATE.columnName,
            EXPIRY_DATE.columnName
    )

    private val query = """
                    SELECT ${projection.joinToString(", ")}
                    FROM $TABLE_CACHE
                    ORDER BY ${CLASS.columnName} ASC
                """

    fun getStatistics() = Single.fromCallable<CacheStatistics> {
        database.query(query)
                .useAndLogError(
                        ::compileStatistics,
                        logger
                )
    }!!

    private fun compileStatistics(cursor: Cursor): CacheStatistics {
        val entries = mutableMapOf<Class<*>, MutableList<CacheEntry>>()

        if (cursor.count != 0 && cursor.moveToNext()) {
            val entry = compileEntry(cursor)
            if (entries[entry.responseClass] == null) {
                entries[entry.responseClass] = mutableListOf()
            }
            entries[entry.responseClass]!!.add(entry)
        }

        val entrySummaries = entries.map {
            val values = it.value
            val fresh = values.count { it.status == FRESH }
            val stale = values.size - fresh
            var oldest: Date? = null
            var latest: Date? = null

            values.forEach {
                with(it.cacheDate) {
                    when {
                        oldest == null && latest == null -> {
                            oldest = this; latest = this
                        }
                        before(oldest) -> oldest = this
                        after(latest) -> latest = this
                    }
                }
            }

            CacheEntrySummary(
                    values.first().responseClass,
                    fresh,
                    stale,
                    oldest!!,
                    latest!!,
                    values
            )
        }

        return CacheStatistics(
                configuration,
                entrySummaries
        )
    }

    private fun compileEntry(cursor: Cursor): CacheEntry {
        val responseClassName = cursor.getString(cursor.getColumnIndex(CLASS.columnName))
        val isEncrypted = cursor.getInt(cursor.getColumnIndex(IS_ENCRYPTED.columnName)) != 0
        val isCompressed = cursor.getInt(cursor.getColumnIndex(IS_COMPRESSED.columnName)) != 0
        val cacheDate = dateFactory(cursor.getLong(cursor.getColumnIndex(DATE.columnName)))
        val expiryDate = dateFactory(cursor.getLong(cursor.getColumnIndex(EXPIRY_DATE.columnName)))

        val status = getCacheStatus(expiryDate, dateFactory)

        return CacheEntry(
                Class.forName(responseClassName),
                status,
                isEncrypted,
                isCompressed,
                cacheDate,
                expiryDate
        )
    }
}