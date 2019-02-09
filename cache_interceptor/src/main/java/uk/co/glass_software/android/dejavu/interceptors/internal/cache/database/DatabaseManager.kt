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
import io.reactivex.Completable.create
import io.requery.android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import uk.co.glass_software.android.boilerplate.utils.io.useAndLogError
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Type.INVALIDATE
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Type.REFRESH
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.SqlOpenHelperCallback.Companion.COLUMNS.*
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.SqlOpenHelperCallback.Companion.TABLE_CACHE
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.Hasher
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

internal class DatabaseManager<E>(private val database: SupportSQLiteDatabase,
                                  private val serialisationManager: SerialisationManager<E>,
                                  private val logger: Logger,
                                  private val hasher: Hasher,
                                  private val compressData: Boolean,
                                  private val encryptData: Boolean,
                                  private val durationInMillis: Long,
                                  private val dateFactory: (Long?) -> Date,
                                  private val contentValuesFactory: (Map<String, *>) -> ContentValues)
        where E : Exception,
              E : NetworkErrorProvider {

    private val dateFormat: DateFormat

    init {
        dateFormat = SimpleDateFormat("MMM dd h:m:s", Locale.UK)
    }

    fun clearCache(typeToClear: Class<*>?,
                   clearOlderEntriesOnly: Boolean) {
        val olderEntriesClause = if (clearOlderEntriesOnly) "${EXPIRY_DATE.columnName} < ?" else null
        val typeClause = typeToClear?.let { "${CLASS.columnName} = ?" }

        val args = arrayListOf<String>().apply {
            if (clearOlderEntriesOnly) add(dateFactory(null).time.toString())
            if (typeToClear != null) add(typeToClear.name)
        }

        database.delete(
                TABLE_CACHE,
                arrayOf(olderEntriesClause, typeClause).filterNotNull().joinToString(separator = " AND "),
                args.toArray()
        ).let { deleted ->
            val entryType = typeToClear?.simpleName?.let { " $it" } ?: ""
            if (clearOlderEntriesOnly) {
                logger.d(this, "Deleted old$entryType entries from cache: $deleted found")
            } else {
                logger.d(this, "Deleted all existing$entryType entries from cache: $deleted found")
            }
        }
    }

    fun getCachedResponse(instructionToken: CacheToken,
                          start: Long): ResponseWrapper<E>? {
        val instruction = instructionToken.instruction
        val simpleName = instruction.responseClass.simpleName
        logger.d(this, "Checking for cached $simpleName")

        val key = hasher.getTokenKey(instructionToken)
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

        database.query(query).useAndLogError { cursor ->
            if (cursor.count != 0 && cursor.moveToNext()) {
                logger.d(this, "Found a cached $simpleName")

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
        }
    }

    fun invalidate(instructionToken: CacheToken) {
        checkInvalidation(
                instructionToken.instruction,
                hasher.getTokenKey(instructionToken)
        )
    }

    private fun checkInvalidation(instruction: CacheInstruction,
                                  key: String) {
        if (instruction.operation.type.let { it == INVALIDATE || it == REFRESH }) {
            val map = HashMap<String, Any>()
            map[EXPIRY_DATE.columnName] = 0

            val selection = "${TOKEN.columnName} = ?"
            val selectionArgs = arrayOf(key)

            database.update(
                    TABLE_CACHE,
                    CONFLICT_REPLACE,
                    contentValuesFactory(map),
                    selection,
                    selectionArgs
            ).let {
                logger.d(this, "Invalidating cache for ${instruction.responseClass.simpleName}: ${if (it > 0) "done" else "nothing found"}")
            }
        }
    }

    private fun getCachedResponse(instructionToken: CacheToken,
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
                    isCompressed
            ) { clearCache(null, false) }
                    ?.also {
                        val callDuration = CacheMetadata.Duration(
                                (dateFactory(null).time - start).toInt(),
                                0,
                                0
                        )

                        it.metadata = CacheMetadata(
                                CacheToken.cached(
                                        instructionToken,
                                        getCachedStatus(expiryDate),
                                        isCompressed,
                                        isEncrypted,
                                        cacheDate,
                                        expiryDate
                                ),
                                null,
                                callDuration
                        )

                        logger.d(this, "Returning cached ${instructionToken.instruction.responseClass.simpleName} cached until ${dateFormat.format(expiryDate)}")
                    }

    @VisibleForTesting
    fun getCachedStatus(expiryDate: Date) =
            if (dateFactory(null).time > expiryDate.time) CacheStatus.STALE else CacheStatus.CACHED

    fun cache(instructionToken: CacheToken,
              cacheOperation: Expiring,
              response: ResponseWrapper<E>,
              previousCachedResponse: ResponseWrapper<E>?) = create {
        val instruction = instructionToken.instruction
        val operation = instruction.operation as Expiring
        val simpleName = instruction.responseClass.simpleName
        val durationInMillis = operation.durationInMillis ?: durationInMillis

        logger.d(this, "Caching $simpleName")

        val (encryptData, compressData) = wasPreviouslyEncrypted(
                previousCachedResponse,
                cacheOperation
        )

        serialisationManager.serialise(
                response,
                encryptData,
                compressData
        )?.also {
            val hash = hasher.getTokenKey(instructionToken)
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
