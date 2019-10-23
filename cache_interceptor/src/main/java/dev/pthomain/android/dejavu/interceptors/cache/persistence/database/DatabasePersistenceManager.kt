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

package dev.pthomain.android.dejavu.interceptors.cache.persistence.database

import android.content.ContentValues
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.pthomain.android.boilerplate.core.utils.io.useAndLogError
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.configuration.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Type.INVALIDATE
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Type.REFRESH
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.BasePersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.CacheDataHolder
import dev.pthomain.android.dejavu.interceptors.cache.persistence.database.SqlOpenHelperCallback.Companion.COLUMNS.*
import dev.pthomain.android.dejavu.interceptors.cache.persistence.database.SqlOpenHelperCallback.Companion.TABLE_DEJA_VU
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationException
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager.Factory.Type.DATABASE
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import io.requery.android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import java.util.*

/**
 * Provides a PersistenceManager implementation saving the responses to a SQLite database.
 *
 * @param database the opened database
 * @param hasher the class handling the request hashing for unicity
 * @param serialisationManager used for the serialisation/deserialisation of the cache entries
 * @param dejaVuConfiguration the global cache configuration
 * @param dateFactory class providing the time, for the purpose of testing
 * @param contentValuesFactory converter from Map to ContentValues for testing purpose
 */
class DatabasePersistenceManager<E> internal constructor(private val database: SupportSQLiteDatabase,
                                                         private val hasher: Hasher,
                                                         serialisationManager: SerialisationManager<E>,
                                                         dejaVuConfiguration: DejaVuConfiguration<E>,
                                                         dateFactory: (Long?) -> Date,
                                                         private val contentValuesFactory: (Map<String, *>) -> ContentValues)
    : BasePersistenceManager<E>(
        dejaVuConfiguration,
        serialisationManager,
        dateFactory
) where E : Exception,
        E : NetworkErrorPredicate {

    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param typeToClear type of entries to clear (or all the entries if this parameter is null)
     * @param clearStaleEntriesOnly only clear STALE entries if set to true (or all otherwise)
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
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
                TABLE_DEJA_VU,
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
     *
     * @return the cached data as a CacheDataHolder
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    override fun getCacheDataHolder(instructionToken: CacheToken,
                                    requestMetadata: RequestMetadata.Hashed): CacheDataHolder.Complete? {
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
            FROM $TABLE_DEJA_VU
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
                                val responseClassHash = cursor.getString(cursor.getColumnIndex(CLASS.columnName))

                                return CacheDataHolder.Complete(
                                        requestMetadata,
                                        cacheDate.time,
                                        expiryDate.time,
                                        localData,
                                        responseClassHash,
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
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    override fun invalidateIfNeeded(instructionToken: CacheToken) =
            if (instructionToken.instruction.operation.type.let { it == INVALIDATE || it == REFRESH }) {
                val map = mapOf(EXPIRY_DATE.columnName to 0)
                val selection = "${TOKEN.columnName} = ?"
                val selectionArgs = arrayOf(instructionToken.requestMetadata.urlHash)

                database.update(
                        TABLE_DEJA_VU,
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
     * @param responseWrapper the response to cache
     * @param previousCachedResponse the previously cached response if available for the purpose of replicating the previous cache settings for the new entry (i.e. compression and encryption)
     *
     * @throws SerialisationException in case the serialisation failed
     */
    @Throws(SerialisationException::class)
    override fun cache(responseWrapper: ResponseWrapper<E>,
                       previousCachedResponse: ResponseWrapper<E>?) {
        serialise(responseWrapper, previousCachedResponse)?.let {
            with(it) {
                val values = HashMap<String, Any>()
                values[TOKEN.columnName] = requestMetadata.urlHash
                values[DATE.columnName] = cacheDate
                values[EXPIRY_DATE.columnName] = expiryDate
                values[DATA.columnName] = data
                values[CLASS.columnName] = requestMetadata.classHash
                values[IS_COMPRESSED.columnName] = ifElse(isCompressed, 1, 0)
                values[IS_ENCRYPTED.columnName] = ifElse(isEncrypted, 1, 0)

                try {
                    database.insert(
                            TABLE_DEJA_VU,
                            CONFLICT_REPLACE,
                            contentValuesFactory(values)
                    )
                } catch (e: Exception) {
                    throw SerialisationException("Could not save the response to database", e)
                }
            }
        }
    }

    //TODO remove this
    class Factory<E> internal constructor(private val database: SupportSQLiteDatabase,
                                          private val hasher: Hasher,
                                          private val serialisationManagerFactory: SerialisationManager.Factory<E>,
                                          private val dejaVuConfiguration: DejaVuConfiguration<E>,
                                          private val dateFactory: (Long?) -> Date,
                                          private val contentValuesFactory: (Map<String, *>) -> ContentValues)
            where E : Exception,
                  E : NetworkErrorPredicate {

        fun create(): PersistenceManager<E> = DatabasePersistenceManager(
                database,
                hasher,
                serialisationManagerFactory.create(DATABASE),
                dejaVuConfiguration,
                dateFactory,
                contentValuesFactory
        )
    }

}
