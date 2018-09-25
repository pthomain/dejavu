package uk.co.glass_software.android.cache_interceptor.interceptors.cache

import android.content.ContentValues
import io.reactivex.Completable.create
import io.requery.android.database.sqlite.SQLiteDatabase
import io.requery.android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.REFRESH
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.SqlOpenHelper.Companion.COLUMNS.*
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.SqlOpenHelper.Companion.TABLE_CACHE
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
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
                "${EXPIRY_DATE.columnName} < ?",
                arrayOf(date)
        )

        logger.d("Cleared $deleted old ${if (deleted > 1) "entries" else "entry"} from HTTP cache")
    }

    fun clearCache() {
        db.execSQL("DELETE FROM $TABLE_CACHE")
        logger.d("Cleared entire HTTP cache")
    }

    fun getCachedResponse(instructionToken: CacheToken): ResponseWrapper<E>? {
        val instruction = instructionToken.instruction
        val simpleName = instruction.responseClass.simpleName
        logger.d("Checking for cached $simpleName")

        val key = instructionToken.getKey(hasher)
        checkInvalidation(instruction, key)

        val projection = arrayOf(
                DATE.columnName,
                EXPIRY_DATE.columnName,
                DATA.columnName,
                IS_COMPRESSED.columnName,
                IS_ENCRYPTED.columnName
        )

        val selection = "${TOKEN.columnName} = ?"
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
                logger.d("Found a cached $simpleName")

                val cacheDate = dateFactory(cursor.getLong(cursor.getColumnIndex(DATE.columnName)))
                val expiryDate = dateFactory(cursor.getLong(cursor.getColumnIndex(EXPIRY_DATE.columnName)))
                val localData = cursor.getBlob(cursor.getColumnIndex(DATA.columnName))
                val isCompressed = cursor.getInt(cursor.getColumnIndex(IS_COMPRESSED.columnName)) != 0
                val isEncrypted = cursor.getInt(cursor.getColumnIndex(IS_ENCRYPTED.columnName)) != 0

                return getCachedResponse(
                        instructionToken,
                        cacheDate,
                        expiryDate,
                        isCompressed,
                        isEncrypted,
                        localData
                )
            } else {
                logger.d("Found no cached $simpleName")
                return null
            }
        }
    }

    private fun checkInvalidation(instruction: CacheInstruction,
                                  key: String) {
        if (instruction.operation.type == REFRESH) {
            val map = HashMap<String, Any>()
            map[EXPIRY_DATE.columnName] = 0

            val selection = "${TOKEN.columnName} = ?"
            val selectionArgs = arrayOf(key)

            val update = db.update(
                    TABLE_CACHE,
                    contentValuesFactory(map),
                    selection,
                    selectionArgs
            )

            logger.d("Invalidating cache for $key: ${if (update > 0) "DONE" else "NOT FOUND"}")
        }
    }

    private fun getCachedResponse(instructionToken: CacheToken,
                                  cacheDate: Date,
                                  expiryDate: Date,
                                  isCompressed: Boolean,
                                  isEncrypted: Boolean,
                                  localData: ByteArray) =
            serialisationManager.deserialise(
                    instructionToken,
                    localData,
                    isEncrypted,
                    isCompressed,
                    this::clearCache
            )?.also {
                it.metadata = CacheMetadata(
                        CacheToken.cached(
                                instructionToken,
                                cacheDate,
                                expiryDate
                        ),
                        null
                )

                logger.d("Returning cached ${instructionToken.instruction.responseClass.simpleName} cached until ${dateFormat.format(expiryDate)}")
            }

    fun cache(instructionToken: CacheToken,
              cacheOperation: Expiring,
              response: ResponseWrapper<E>) = create {
        val instruction = instructionToken.instruction
        val operation = instruction.operation as Expiring
        val simpleName = instruction.responseClass.simpleName

        logger.d("Caching $simpleName")

        serialisationManager.serialise(
                response,
                cacheOperation.encrypt,
                cacheOperation.compress
        )?.also {
            val hash = instructionToken.getKey(hasher)
            val values = HashMap<String, Any>()
            val now = System.currentTimeMillis()

            values[TOKEN.columnName] = hash
            values[DATE.columnName] = now
            values[EXPIRY_DATE.columnName] = now + operation.durationInMillis
            values[DATA.columnName] = it
            values[IS_COMPRESSED.columnName] = if (cacheOperation.compress) 1 else 0
            values[IS_ENCRYPTED.columnName] = if (cacheOperation.encrypt) 1 else 0

            db.insertWithOnConflict(
                    TABLE_CACHE,
                    null,
                    contentValuesFactory(values),
                    CONFLICT_REPLACE
            )
        } ?: logger.e("Could not serialise and store data for $simpleName")

        it.onComplete()
    }

    companion object {
        private const val DEFAULT_CLEANUP_THRESHOLD_IN_MINUTES = (7 * 24 * 60).toLong() // 1 week
        private val CLEANUP_DATE_FORMAT = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.UK
        )
    }

}
