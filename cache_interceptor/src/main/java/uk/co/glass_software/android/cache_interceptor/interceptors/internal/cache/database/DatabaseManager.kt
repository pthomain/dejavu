package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database

import android.content.ContentValues
import io.reactivex.Completable.create
import io.requery.android.database.sqlite.SQLiteDatabase
import io.requery.android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import uk.co.glass_software.android.boilerplate.utils.io.useAndLogError
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.INVALIDATE
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.REFRESH
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database.SqlOpenHelper.Companion.COLUMNS.*
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database.SqlOpenHelper.Companion.TABLE_CACHE
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation.Hasher
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

internal class DatabaseManager<E>(private val databaseProvider: () -> SQLiteDatabase,
                                  private val serialisationManager: SerialisationManager<E>,
                                  private val logger: Logger,
                                  private val compressData: Boolean,
                                  private val encryptData: Boolean,
                                  private val durationInMillis: Long,
                                  private val dateFactory: (Long) -> Date,
                                  private val contentValuesFactory: (Map<String, *>) -> ContentValues)
        where E : Exception,
              E : NetworkErrorProvider {

    private val dateFormat: DateFormat
    private val hasher: Hasher

    init {
        dateFormat = SimpleDateFormat("MMM dd h:m:s", Locale.UK)
        hasher = CacheToken.getHasher(logger)
    }

    fun clearCache(typeToClear: Class<*>?,
                   clearOlderEntriesOnly: Boolean) {
        val olderEntriesClause = if (clearOlderEntriesOnly) "${EXPIRY_DATE.columnName} < ?" else null
        val typeClause = typeToClear?.let { "${CLASS.columnName} = ?" }

        val args = arrayListOf<String>().apply {
            if (clearOlderEntriesOnly) add(System.currentTimeMillis().toString())
            if (typeToClear != null) add(typeToClear.name)
        }

        databaseProvider().useAndLogError {
            it.delete(
                    TABLE_CACHE,
                    arrayOf(olderEntriesClause, typeClause).filterNotNull().joinToString(separator = " AND "),
                    args.toArray()
            )
        }.let { deleted ->
            val entryType = typeToClear?.simpleName?.let { " $it" } ?: ""
            if (clearOlderEntriesOnly) {
                logger.d("Deleted old$entryType entries from cache: $deleted found")
            } else {
                logger.d("Deleted all existing$entryType entries from cache: $deleted found")
            }
        }
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

        databaseProvider().useAndLogError {
            it.query(
                    TABLE_CACHE,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null,
                    "1"
            ).useAndLogError { cursor ->
                if (cursor.count != 0 && cursor.moveToNext()) {
                    logger.d("Found a cached $simpleName")

                    val cacheDate = dateFactory(cursor.getLong(cursor.getColumnIndex(DATE.columnName)))
                    val localData = cursor.getBlob(cursor.getColumnIndex(DATA.columnName))
                    val isCompressed = cursor.getInt(cursor.getColumnIndex(IS_COMPRESSED.columnName)) != 0
                    val isEncrypted = cursor.getInt(cursor.getColumnIndex(IS_ENCRYPTED.columnName)) != 0

                    val expiryDate = dateFactory(
                            if (instruction.operation is Expiring.Refresh) 0L
                            else cursor.getLong(cursor.getColumnIndex(EXPIRY_DATE.columnName))
                    )

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
    }

    private fun checkInvalidation(instruction: CacheInstruction,
                                  key: String) {
        if (instruction.operation.type.let { it == INVALIDATE || it == REFRESH }) {
            val map = HashMap<String, Any>()
            map[EXPIRY_DATE.columnName] = 0

            val selection = "${TOKEN.columnName} = ?"
            val selectionArgs = arrayOf(key)

            databaseProvider().useAndLogError {
                it.update(
                        TABLE_CACHE,
                        contentValuesFactory(map),
                        selection,
                        selectionArgs
                ).let {
                    logger.d("Invalidating cache for $key: ${if (it > 0) "DONE" else "NOT FOUND"}")
                }
            }
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
                    isCompressed
            ) { clearCache(null, false) }
                    ?.also {
                        it.metadata = CacheMetadata(
                                CacheToken.cached(
                                        instructionToken,
                                        isCompressed,
                                        isEncrypted,
                                        cacheDate,
                                        expiryDate
                                ),
                                null
                        )

                        logger.d("Returning cached ${instructionToken.instruction.responseClass.simpleName} cached until ${dateFormat.format(expiryDate)}")
                    }

    fun cache(instructionToken: CacheToken,
              cacheOperation: Expiring,
              response: ResponseWrapper<E>,
              previousCachedResponse: ResponseWrapper<E>?) = create {
        val instruction = instructionToken.instruction
        val operation = instruction.operation as Expiring
        val simpleName = instruction.responseClass.simpleName
        val durationInMillis = operation.durationInMillis ?: durationInMillis

        logger.d("Caching $simpleName")

        val (encryptData, compressData) = wasPreviouslyEncrypted(
                previousCachedResponse,
                cacheOperation
        )

        serialisationManager.serialise(
                response,
                encryptData,
                compressData
        )?.also {
            val hash = instructionToken.getKey(hasher)
            val values = HashMap<String, Any>()
            val now = System.currentTimeMillis()

            values[TOKEN.columnName] = hash
            values[DATE.columnName] = now
            values[EXPIRY_DATE.columnName] = now + durationInMillis
            values[DATA.columnName] = it
            values[CLASS.columnName] = instruction.responseClass.name
            values[IS_COMPRESSED.columnName] = if (compressData) 1 else 0
            values[IS_ENCRYPTED.columnName] = if (encryptData) 1 else 0

            databaseProvider().useAndLogError {
                it.insertWithOnConflict(
                        TABLE_CACHE,
                        null,
                        contentValuesFactory(values),
                        CONFLICT_REPLACE
                )
            }
        } ?: logger.e("Could not serialise and store data for $simpleName")

        it.onComplete()
    }!!

    internal fun wasPreviouslyEncrypted(previousCachedResponse: ResponseWrapper<E>?,
                                        cacheOperation: Expiring): Pair<Boolean, Boolean> {
        val previousCacheToken = previousCachedResponse?.metadata?.cacheToken

        return if (previousCacheToken != null) {
            Pair(
                    previousCacheToken.isEncrypted,
                    previousCacheToken.isCompressed
            )
        } else {
            Pair(
                    cacheOperation.encrypt ?: this.encryptData,
                    cacheOperation.compress ?: this.compressData
            )
        }
    }

}
