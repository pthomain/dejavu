/*
 *
 *  Copyright (C) 2017-2020 Pierre Thomain
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

package dev.pthomain.android.dejavu.persistence.sqlite

import android.content.ContentValues
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.pthomain.android.boilerplate.core.utils.io.useAndLogError
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.persistence.base.BasePersistenceManager
import dev.pthomain.android.dejavu.persistence.base.CacheDataHolder
import dev.pthomain.android.dejavu.persistence.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.persistence.sqlite.SqlOpenHelperCallback.Companion.COLUMNS.*
import dev.pthomain.android.dejavu.persistence.sqlite.SqlOpenHelperCallback.Companion.TABLE_DEJA_VU
import dev.pthomain.android.dejavu.shared.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.shared.serialisation.SerialisationException
import dev.pthomain.android.dejavu.shared.token.CacheToken
import dev.pthomain.android.dejavu.shared.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.shared.token.instruction.ValidRequestMetadata
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Remote.Cache
import io.requery.android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import java.util.*

/**
 * Provides a PersistenceManager implementation saving the responses to a SQLite database.
 *
 * @param database the opened database
 * @param serialisationManager used for the serialisation/deserialisation of the cache entries
 * @param dateFactory class providing the time, for the purpose of testing
 * @param contentValuesFactory converter from Map to ContentValues for testing purpose
 */
class DatabasePersistenceManager internal constructor(
        private val database: SupportSQLiteDatabase,
        logger: Logger,
        serialisationManager: SerialisationManager,
        dateFactory: (Long?) -> Date,
        private val contentValuesFactory: (Map<String, *>) -> ContentValues
) : BasePersistenceManager(
        logger,
        serialisationManager,
        dateFactory
) {

    override val decorator: SerialisationDecorator? = null

    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param operation the Clear operation
     * @param requestMetadata the request's metadata
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> clearCache(
            operation: Clear,
            requestMetadata: ValidRequestMetadata<R>
    ) {
        val olderEntriesClause = ifElse(
                operation.clearStaleEntriesOnly,
                "${EXPIRY_DATE.columnName} < ?",
                null
        )

        val typeClause = "${CLASS.columnName} = ?"

        val args = arrayListOf<String>().apply {
            if (operation.clearStaleEntriesOnly) add(dateFactory(null).time.toString())
            add(requestMetadata.classHash) //TODO requestHash
        }

        database.delete(
                TABLE_DEJA_VU,
                arrayOf(olderEntriesClause, typeClause).filterNotNull().joinToString(separator = " AND "),
                args.toArray()
        ).let { deleted ->
            val entryType = requestMetadata.responseClass.simpleName
            if (operation.clearStaleEntriesOnly) {
                logger.d(this, "Deleted old $entryType entries from cache: $deleted found")
            } else {
                logger.d(this, "Deleted all existing $entryType entries from cache: $deleted found")
            }
        }
    }

    /**
     * Returns the cached data as a CacheDataHolder object.
     *
     * @param requestMetadata the associated request metadata
     *
     * @return the cached data as a CacheDataHolder
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> getCacheDataHolder(requestMetadata: HashedRequestMetadata<R>): CacheDataHolder? {
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
            WHERE ${TOKEN.columnName} = '${requestMetadata.requestHash}'
            LIMIT 1
            """

        database.query(query)
                .useAndLogError(
                        {
                            with(it) {
                                val simpleName = requestMetadata.responseClass.simpleName
                                if (count != 0 && moveToNext()) {
                                    logger.d(this, "Found a cached $simpleName")

                                    val cacheDate = dateFactory(getLong(getColumnIndex(DATE.columnName)))
                                    val localData = getBlob(getColumnIndex(DATA.columnName))
                                    val isCompressed = getInt(getColumnIndex(IS_COMPRESSED.columnName)) != 0
                                    val isEncrypted = getInt(getColumnIndex(IS_ENCRYPTED.columnName)) != 0
                                    val expiryDate = dateFactory(getLong(getColumnIndex(EXPIRY_DATE.columnName)))
                                    val responseClassHash = getString(getColumnIndex(CLASS.columnName))
                                    //TODO verify the class hash is the same as the one the request metadata

                                    return CacheDataHolder.Complete(
                                            requestMetadata,
                                            cacheDate.time,
                                            expiryDate.time,
                                            localData,
                                            isCompressed,
                                            isEncrypted
                                    )
                                } else {
                                    logger.d(this, "Found no cached $simpleName")
                                    return null
                                }
                            }
                        },
                        logger
                )
    }

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE).
     *
     * @param requestMetadata the request's metadata
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    override fun <R : Any> forceInvalidation(requestMetadata: ValidRequestMetadata<R>): Boolean {
        val map = mapOf(EXPIRY_DATE.columnName to 0)
        val selection = "${TOKEN.columnName} = ?"
        val selectionArgs = arrayOf(requestMetadata.requestHash) //TODO classHash

        val results = database.update(
                TABLE_DEJA_VU,
                CONFLICT_REPLACE,
                contentValuesFactory(map),
                selection,
                selectionArgs
        )

        val foundIt = results > 0

        logger.d(
                this,
                "Invalidating cache for ${requestMetadata.responseClass.simpleName}: ${if (foundIt) "done" else "nothing found"}"
        )

        return foundIt
    }

    /**
     * Caches a given response.
     *
     * @param responseWrapper the response to cache
     * @throws SerialisationException in case the serialisation failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> cache(
            response: R,
            instructionToken: CacheToken<Cache, R>,
    ) {
        with(serialise(response, instructionToken)) {
            val values = HashMap<String, Any>()
            values[TOKEN.columnName] = requestMetadata.requestHash
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
