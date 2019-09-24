/*
 *
 *  Copyright (C) 2017 Pierre Thomain
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.database

import android.content.ContentValues
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.pthomain.android.boilerplate.core.utils.io.useAndLogError
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Type.INVALIDATE
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Type.REFRESH
import dev.pthomain.android.dejavu.configuration.NetworkErrorProvider
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.BasePersistenceManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.CacheDataHolder
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.database.SqlOpenHelperCallback.Companion.COLUMNS.*
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.database.SqlOpenHelperCallback.Companion.TABLE_CACHE
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.token.CacheToken
import dev.pthomain.android.dejavu.response.ResponseWrapper
import io.requery.android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import java.util.*

/**
 * Handles database operations
 *
 * @param database the opened database
 * @param serialisationManager used for the serialisation/deserialisation of the cache entries
 * @param cacheConfiguration the global cache configuration
 * @param dateFactory class providing the time, for the purpose of testing
 * @param contentValuesFactory converter from Map to ContentValues for testing purpose
 */
internal class DatabasePersistenceManager<E>(private val database: SupportSQLiteDatabase,
                                             private val hasher: Hasher,
                                             serialisationManager: SerialisationManager<E>,
                                             cacheConfiguration: CacheConfiguration<E>,
                                             dateFactory: (Long?) -> Date,
                                             private val contentValuesFactory: (Map<String, *>) -> ContentValues)
    : BasePersistenceManager<E>(
        cacheConfiguration,
        serialisationManager,
        dateFactory
) where E : Exception,
        E : NetworkErrorProvider {

    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param typeToClear type of entries to clear (or all the entries if this parameter is null)
     * @param clearStaleEntriesOnly only clear STALE entries if set to true (or all otherwise)
     */
    override fun clearCache(typeToClear: Class<*>?,
                            clearStaleEntriesOnly: Boolean) {
        val olderEntriesClause = ifElse(
                clearStaleEntriesOnly,
                "${EXPIRY_DATE.columnName} < ?",
                null
        )

        val typeClause = typeToClear?.let { "${CLASS.columnName} = ?" }

        val args = arrayListOf<String>().apply {
            if (clearStaleEntriesOnly) add(dateFactory(null).time.toString())

            if (typeToClear != null) {
                val classHash = hasher.hash(typeToClear.name)
                add(classHash)
            }
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
     * Returns the cached data as a CacheDataHolder object.
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     * @param requestMetadata the associated request metadata
     * @param start the time at which the operation started in order to calculate the time the operation took.
     *
     * @return the cached data as a CacheDataHolder
     */
    override fun getCacheDataHolder(
            instructionToken: CacheToken,
            requestMetadata: RequestMetadata.Hashed,
            start: Long
    ): CacheDataHolder? {
        val projection = arrayOf(
                DATE.columnName,
                EXPIRY_DATE.columnName,
                DATA.columnName,
                IS_COMPRESSED.columnName,
                IS_ENCRYPTED.columnName,
                CLASS.columnName
        )

        val query = """
            SELECT ${projection.joinToString(", ")}
            FROM $TABLE_CACHE
            WHERE ${TOKEN.columnName} = '${requestMetadata.urlHash}'
            LIMIT 1
            """

        database.query(query)
                .useAndLogError(
                        { cursor ->
                            val simpleName = instructionToken.instruction.responseClass.simpleName
                            if (cursor.count != 0 && cursor.moveToNext()) {
                                logger.d(this, "Found a cached $simpleName")

                                val cacheDate = dateFactory(cursor.getLong(cursor.getColumnIndex(DATE.columnName)))
                                val localData = cursor.getBlob(cursor.getColumnIndex(DATA.columnName))
                                val isCompressed = cursor.getInt(cursor.getColumnIndex(IS_COMPRESSED.columnName)) != 0
                                val isEncrypted = cursor.getInt(cursor.getColumnIndex(IS_ENCRYPTED.columnName)) != 0
                                val expiryDate = dateFactory(cursor.getLong(cursor.getColumnIndex(EXPIRY_DATE.columnName)))
                                val className = cursor.getString(cursor.getColumnIndex(CLASS.columnName))

                                return CacheDataHolder(
                                        requestMetadata,
                                        cacheDate.time,
                                        expiryDate.time,
                                        localData,
                                        className,
                                        isCompressed,
                                        isEncrypted
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
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     *
     * @param instructionToken the INVALIDATE instruction token for the desired entry.
     * @param key the key of the entry to invalidate
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    override fun checkInvalidation(instructionToken: CacheToken) =
            if (instructionToken.instruction.operation.type.let { it == INVALIDATE || it == REFRESH }) {
                val map = mapOf(EXPIRY_DATE.columnName to 0)
                val selection = "${TOKEN.columnName} = ?"
                val selectionArgs = arrayOf(instructionToken.requestMetadata.urlHash)

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
                            "Invalidating cache for ${instructionToken.instruction.responseClass.simpleName}: ${if (foundIt) "done" else "nothing found"}"
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
    override fun cache(response: ResponseWrapper<E>,
                       previousCachedResponse: ResponseWrapper<E>?) {
        with(serialise(response, previousCachedResponse)) {
            if (this != null) {
                val values = HashMap<String, Any>()
                values[TOKEN.columnName] = requestMetadata!!.urlHash
                values[DATE.columnName] = cacheDate
                values[EXPIRY_DATE.columnName] = expiryDate
                values[DATA.columnName] = data
                values[CLASS.columnName] = requestMetadata.classHash
                values[IS_COMPRESSED.columnName] = ifElse(isCompressed, 1, 0)
                values[IS_ENCRYPTED.columnName] = ifElse(isEncrypted, 1, 0)

                database.insert(
                        TABLE_CACHE,
                        CONFLICT_REPLACE,
                        contentValuesFactory(values)
                )
            }
        }
    }
}