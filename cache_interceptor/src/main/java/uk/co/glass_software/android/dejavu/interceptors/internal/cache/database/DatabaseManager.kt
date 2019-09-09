/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.dejavu.interceptors.internal.cache.database

import android.content.ContentValues
import androidx.annotation.VisibleForTesting
import androidx.sqlite.db.SupportSQLiteDatabase
import io.reactivex.Completable
import io.reactivex.Completable.create
import io.requery.android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import uk.co.glass_software.android.boilerplate.core.utils.io.useAndLogError
import uk.co.glass_software.android.boilerplate.core.utils.lambda.Action.Companion.act
import uk.co.glass_software.android.boilerplate.core.utils.log.Logger
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Invalidate
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Type.INVALIDATE
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Type.REFRESH
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.SqlOpenHelperCallback.Companion.COLUMNS.*
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.SqlOpenHelperCallback.Companion.TABLE_CACHE
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus.FRESH
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus.STALE
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handles database operations
 *
 * @param database the opened database
 * @param serialisationManager used for the serialisation/deserialisation of the cache entries
 * @param logger an Logger instance
 * @param compressDataGlobally whether or not the data should be compressed by default if no entry-specific directive exists (as defined in CacheConfiguration)
 * @param encryptDataGlobally whether or not the data should be encrypted by default if no entry-specific directive exists (as defined in CacheConfiguration)
 * @param durationInMillis interval during which the cached data should be considered fresh from the time when it is saved
 * @param dateFactory class providing the time, for the purpose of testing
 * @param contentValuesFactory converter from Map to ContentValues for testing purpose
 */
internal class DatabaseManager<E>(private val database: SupportSQLiteDatabase,
                                  private val serialisationManager: SerialisationManager<E>,
                                  private val logger: Logger,
                                  private val compressDataGlobally: Boolean,
                                  private val encryptDataGlobally: Boolean,
                                  private val durationInMillis: Long,
                                  private val dateFactory: (Long?) -> Date,
                                  private val contentValuesFactory: (Map<String, *>) -> ContentValues)
        where E : Exception,
              E : NetworkErrorProvider {

    private val dateFormat: DateFormat

    init {
        dateFormat = SimpleDateFormat("MMM dd h:m:s", Locale.UK)
    }

    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param typeToClear type of entries to clear (or all the entries if this parameter is null)
     * @param clearStaleEntriesOnly only clear STALE entries if set to true (or all otherwise)
     */
    fun clearCache(typeToClear: Class<*>?,
                   clearStaleEntriesOnly: Boolean) {
        val olderEntriesClause = if (clearStaleEntriesOnly) "${EXPIRY_DATE.columnName} < ?" else null
        val typeClause = typeToClear?.let { "${CLASS.columnName} = ?" }

        val args = arrayListOf<String>().apply {
            if (clearStaleEntriesOnly) add(dateFactory(null).time.toString())
            if (typeToClear != null) add(typeToClear.name)
        }

        database.delete(
                TABLE_CACHE,
                arrayOf(olderEntriesClause, typeClause).filterNotNull().joinToString(separator = " AND "),
                args.toArray()
        ).let { deleted ->
            val entryType = typeToClear?.simpleName?.let { " $it" } ?: ""
            if (clearStaleEntriesOnly) {
                logger.d(this, "Deleted old$entryType entries from cache: $deleted found")
            } else {
                logger.d(this, "Deleted all existing$entryType entries from cache: $deleted found")
            }
        }
    }

    /**
     * Returns a cached entry if available
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     * @param start the time at which the operation started in order to calculate the time the operation took.
     *
     * @return a cached entry if available, or null otherwise
     */
    fun getCachedResponse(instructionToken: CacheToken,
                          start: Long): ResponseWrapper<E>? {
        val instruction = instructionToken.instruction
        val simpleName = instruction.responseClass.simpleName
        logger.d(this, "Checking for cached $simpleName")

        val key = instructionToken.requestMetadata.hash
        checkInvalidation(instruction, key)

        val projection = arrayOf(
                DATE.columnName,
                EXPIRY_DATE.columnName,
                DATA.columnName,
                IS_COMPRESSED.columnName,
                IS_ENCRYPTED.columnName
        )

        val query = """
            SELECT ${projection.joinToString(", ")}
            FROM $TABLE_CACHE
            WHERE ${TOKEN.columnName} = '$key'
            LIMIT 1
            """

        database.query(query)
                .useAndLogError(
                        { cursor ->
                            if (cursor.count != 0 && cursor.moveToNext()) {
                                logger.d(this, "Found a cached $simpleName")

                                val cacheDate = dateFactory(cursor.getLong(cursor.getColumnIndex(DATE.columnName)))
                                val localData = cursor.getBlob(cursor.getColumnIndex(DATA.columnName))
                                val isCompressed = cursor.getInt(cursor.getColumnIndex(IS_COMPRESSED.columnName)) != 0
                                val isEncrypted = cursor.getInt(cursor.getColumnIndex(IS_ENCRYPTED.columnName)) != 0
                                val expiryDate = dateFactory(cursor.getLong(cursor.getColumnIndex(EXPIRY_DATE.columnName)))

                                return deserialise(
                                        instructionToken,
                                        start,
                                        cacheDate,
                                        expiryDate,
                                        isCompressed,
                                        isEncrypted,
                                        localData
                                )
                            } else {
                                logger.d(this, "Found no cached $simpleName")
                                return null
                            }
                        },
                        logger
                )
    }

    /**
     * Deserialises the cached data
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     * @param start the time at which the operation started in order to calculate the time the operation took.
     * @param cacheDate the Date at which the data was cached
     * @param expiryDate the Date at which the data would become STALE
     * @param isCompressed whether or not the cached data was saved compressed
     * @param isEncrypted whether or not the cached data was saved encrypted
     * @param localData the cached data
     *
     * @return a ResponseWrapper containing the deserialised data or null if the operation failed.
     */
    private fun deserialise(instructionToken: CacheToken,
                            start: Long,
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
                    { clearCache(null, false) }.act()
            )?.let {
                val callDuration = CacheMetadata.Duration(
                        (dateFactory(null).time - start).toInt(),
                        0,
                        0
                )

                logger.d(
                        this,
                        "Returning cached ${instructionToken.instruction.responseClass.simpleName} cached until ${dateFormat.format(expiryDate)}"
                )
                it.apply {
                    metadata = CacheMetadata(
                            CacheToken.cached(
                                    instructionToken,
                                    getCacheStatus(expiryDate, dateFactory),
                                    isCompressed,
                                    isEncrypted,
                                    cacheDate,
                                    expiryDate
                            ),
                            null,
                            callDuration
                    )
                }
            }

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    fun invalidate(instructionToken: CacheToken): Boolean {
        val instruction = instructionToken.instruction.copy(
                operation = Invalidate
        )
        return checkInvalidation(
                instruction,
                instructionToken.requestMetadata.hash
        )
    }

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     *
     * @param instruction the INVALIDATE instruction for the desired entry.
     * @param key the key of the entry to invalidate
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    @VisibleForTesting
    fun checkInvalidation(instruction: CacheInstruction,
                          key: String) =
            if (instruction.operation.type.let { it == INVALIDATE || it == REFRESH }) {
                val map = mapOf(EXPIRY_DATE.columnName to 0)
                val selection = "${TOKEN.columnName} = ?"
                val selectionArgs = arrayOf(key)

                database.update(
                        TABLE_CACHE,
                        CONFLICT_REPLACE,
                        contentValuesFactory(map),
                        selection,
                        selectionArgs
                ).let {
                    val foundIt = it > 0
                    logger.d(
                            this,
                            "Invalidating cache for ${instruction.responseClass.simpleName}: ${if (foundIt) "done" else "nothing found"}"
                    )
                    foundIt
                }
            } else false

    /**
     * Caches a given response.
     *
     * @param response the response to cache
     * @param previousCachedResponse the previously cached response if available for the purpose of replicating the previous cache settings for the new entry (i.e. compression and encryption)
     */
    fun cache(response: ResponseWrapper<E>,
              previousCachedResponse: ResponseWrapper<E>?): Completable {
        val instructionToken = response.metadata.cacheToken

        return create {
            val instruction = instructionToken.instruction
            val operation = instruction.operation as Expiring
            val simpleName = instruction.responseClass.simpleName
            val durationInMillis = operation.durationInMillis ?: durationInMillis

            logger.d(this, "Caching $simpleName")

            val (encryptData, compressData) = shouldEncryptOrCompress(
                    previousCachedResponse,
                    operation
            )

            serialisationManager.serialise(
                    response,
                    encryptData,
                    compressData
            )?.also {
                val hash = instructionToken.requestMetadata.hash
                val values = HashMap<String, Any>()
                val now = dateFactory(null).time

                values[TOKEN.columnName] = hash
                values[DATE.columnName] = now
                values[EXPIRY_DATE.columnName] = now + durationInMillis
                values[DATA.columnName] = it
                values[CLASS.columnName] = instruction.responseClass.name
                values[IS_COMPRESSED.columnName] = if (compressData) 1 else 0
                values[IS_ENCRYPTED.columnName] = if (encryptData) 1 else 0

                database.insert(
                        TABLE_CACHE,
                        CONFLICT_REPLACE,
                        contentValuesFactory(values)
                )
            } ?: logger.e(this, "Could not serialise and store data for $simpleName")

            it.onComplete()
        }!!
    }

    /**
     * Indicates whether or not the entry should be compressed or encrypted based primarily
     * on the settings of the previous cached entry if available. If there was no previous entry,
     * then the cache settings are defined by the operation or, if undefined in the operation,
     * by the values defined globally in CacheConfiguration.
     *
     * @param previousCachedResponse the previously cached response if available for the purpose of replicating the previous cache settings for the new entry (i.e. compression and encryption)
     * @param cacheOperation the cache operation for the entry being saved
     *
     * @return a pair of Boolean indicating in order whether the data was encrypted or compressed
     */
    internal fun shouldEncryptOrCompress(previousCachedResponse: ResponseWrapper<E>?,
                                         cacheOperation: Expiring): Pair<Boolean, Boolean> {
        val previousCacheToken = previousCachedResponse?.metadata?.cacheToken

        return if (previousCacheToken != null) {
            Pair(
                    previousCacheToken.isEncrypted,
                    previousCacheToken.isCompressed
            )
        } else {
            Pair(
                    cacheOperation.encrypt ?: this.encryptDataGlobally,
                    cacheOperation.compress ?: this.compressDataGlobally
            )
        }
    }

}

/**
 * Calculates the cache status of a given expiry date.
 *
 * @param expiryDate the date at which the data should expire (become STALE)
 * @param dateFactory class providing the time, for the purpose of testing
 *
 * @return whether the data is FRESH or STALE
 */
internal fun getCacheStatus(expiryDate: Date,
                            dateFactory: (Long?) -> Date) =
        if (dateFactory(null).time >= expiryDate.time) STALE else FRESH