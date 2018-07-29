package uk.co.glass_software.android.cache_interceptor.interceptors.cache

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers.compressedData
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.REFRESH
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.SqlOpenHelper.Companion.COLUMN_CACHE_DATA
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.SqlOpenHelper.Companion.COLUMN_CACHE_DATE
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.SqlOpenHelper.Companion.COLUMN_CACHE_EXPIRY_DATE
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.SqlOpenHelper.Companion.COLUMN_CACHE_TOKEN
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.SqlOpenHelper.Companion.TABLE_CACHE
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import uk.co.glass_software.android.shared_preferences.utils.Logger
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

internal class DatabaseManager<E>(private val db: SQLiteDatabase,
                                  private val serialisationManager: SerialisationManager<E>,
                                  private val logger: Logger,
                                  cleanUpThresholdInMinutes: Long,
                                  private val dateFactory: (Long) -> Date,
                                  private val contentValuesFactory: (Map<String, *>) -> ContentValues)
        where E : Exception,
              E : (E) -> Boolean {

    private val dateFormat: DateFormat
    private val cleanUpThresholdInMillis = cleanUpThresholdInMinutes * 60 * 1000
    private val hasher: Hasher

    constructor(db: SQLiteDatabase,
                serialisationManager: SerialisationManager<E>,
                logger: Logger,
                dateFactory: (Long) -> Date,
                contentValuesFactory: (Map<String, *>) -> ContentValues) : this(
            db,
            serialisationManager,
            logger,
            DEFAULT_CLEANUP_THRESHOLD_IN_MINUTES,
            dateFactory,
            contentValuesFactory
    )

    init {
        dateFormat = SimpleDateFormat("MMM dd h:m:s", Locale.UK)
        hasher = CacheToken.getHasher(logger)
    }

    fun clearOlderEntries() {
        val date = (System.currentTimeMillis() + cleanUpThresholdInMillis).toString()
        val deleted = db.delete(
                TABLE_CACHE,
                "$COLUMN_CACHE_EXPIRY_DATE < ?",
                arrayOf(date)
        )

        logger.d(this, "Cleared $deleted old ${if (deleted > 1) "entries" else "entry"} from HTTP cache")
    }

    fun flushCache() {
        db.execSQL("DELETE FROM $TABLE_CACHE")
        logger.d(this, "Cleared entire HTTP cache")
    }

    fun getCachedResponse(instructionToken: CacheToken): ResponseWrapper<E>? {
        val instruction = instructionToken.instruction
        val simpleName = instruction.responseClass.simpleName
        logger.d(this, "Checking for cached $simpleName")

        val key = instructionToken.getKey(hasher)
        checkInvalidation(instruction, key)

        val projection = arrayOf(
                COLUMN_CACHE_DATE,
                COLUMN_CACHE_EXPIRY_DATE,
                COLUMN_CACHE_DATA
        )

        val selection = "$COLUMN_CACHE_TOKEN = ?"
        val selectionArgs = arrayOf(key)

        val cursor = db.query(
                TABLE_CACHE,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null,
                "1"
        )

        cursor.use {
            if (it.count != 0 && it.moveToNext()) {
                logger.d(this, "Found a cached $simpleName")

                val cacheDate = dateFactory(cursor.getLong(0))
                val expiryDate = dateFactory(cursor.getLong(1))
                val compressedData = cursor.getBlob(2)

                return getCachedResponse(
                        instructionToken,
                        cacheDate,
                        expiryDate,
                        compressedData
                )
            } else {
                logger.d(this, "Found no cached $simpleName")
                return null
            }
        }
    }

    private fun checkInvalidation(instruction: CacheInstruction,
                                  key: String) {
        if (instruction.operation.type == REFRESH) {
            val map = HashMap<String, Any>()
            map[COLUMN_CACHE_EXPIRY_DATE] = 0

            val selection = "$COLUMN_CACHE_TOKEN = ?"
            val selectionArgs = arrayOf(key)

            val update = db.update(
                    TABLE_CACHE,
                    contentValuesFactory(map),
                    selection,
                    selectionArgs
            )

            logger.d(
                    this,
                    "Invalidating cache for $key: ${if (update > 0) "DONE" else "NOT FOUND"}"
            )
        }
    }

    private fun getCachedResponse(instructionToken: CacheToken,
                                  cacheDate: Date,
                                  expiryDate: Date,
                                  compressedData: ByteArray): ResponseWrapper<E>? {
        val responseWrapper = serialisationManager.deserialise(
                instructionToken.instruction.responseClass,
                compressedData,
                this::flushCache
        )?.also {
            it.metadata = CacheMetadata(
                    CacheToken.cached(
                            instructionToken,
                            cacheDate,
                            expiryDate
                    ),
                    null
            )
        }

        logger.d(
                this,
                "Returning cached ${instructionToken.instruction.responseClass.simpleName} cached until ${dateFormat.format(expiryDate)}"
        )

        return responseWrapper
    }

    fun cache(instructionToken: CacheToken,
              response: ResponseWrapper<E>) {
        val instruction = instructionToken.instruction
        val simpleName = instruction.responseClass.simpleName
        logger.d(this, "Caching $simpleName")

        serialisationManager.serialise(response)?.also {
            val hash = instructionToken.getKey(hasher)
            val values = HashMap<String, Any>()

            values[COLUMN_CACHE_TOKEN] = hash
            values[COLUMN_CACHE_DATE] = instructionToken.cacheDate!!.time
            values[COLUMN_CACHE_EXPIRY_DATE] = instructionToken.expiryDate!!.time
            values[COLUMN_CACHE_DATA] = it

            db.insertWithOnConflict(
                    TABLE_CACHE,
                    null,
                    contentValuesFactory(values),
                    CONFLICT_REPLACE
            )
        } ?: logger.e(this, "Could not serialise and store data for $simpleName")
    }

    companion object {
        private const val DEFAULT_CLEANUP_THRESHOLD_IN_MINUTES = (7 * 24 * 60).toLong() // 1 week
        private val CLEANUP_DATE_FORMAT = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.UK
        )
    }

}
