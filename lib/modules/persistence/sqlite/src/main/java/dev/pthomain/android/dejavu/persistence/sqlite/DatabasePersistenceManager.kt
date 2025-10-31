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
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear.Scope
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.di.DateFactory
import dev.pthomain.android.dejavu.persistence.Persisted.Serialised
import dev.pthomain.android.dejavu.persistence.base.BasePersistenceManager
import dev.pthomain.android.dejavu.persistence.sqlite.SqlOpenHelperCallback.Companion.COLUMNS.*
import dev.pthomain.android.dejavu.persistence.sqlite.SqlOpenHelperCallback.Companion.TABLE_DEJA_VU
import dev.pthomain.android.dejavu.serialisation.SerialisationException
import dev.pthomain.android.dejavu.serialisation.SerialisationManager
import io.requery.android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import java.io.Closeable
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
        dateFactory: DateFactory,
        private val contentValuesFactory: (Map<String, *>) -> ContentValues
) : BasePersistenceManager(
        logger,
        serialisationManager,
        dateFactory
) {

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
            requestMetadata: HashedRequestMetadata<R>,
            operation: Clear
    ) {
        val olderEntriesClause = ifElse(
                operation.clearStaleEntriesOnly,
                "${EXPIRY_DATE.columnName} < ?",
                null
        )

        val requestClause = when (operation.scope) {
            Scope.REQUEST -> "${REQUEST.columnName} = ?"
            Scope.CLASS -> "${CLASS.columnName} = ?"
            Scope.ALL -> null
        }

        val args = arrayListOf<String>().apply {
            if (operation.clearStaleEntriesOnly) add(dateFactory(null).time.toString())
            if (requestClause != null) {
                add(when (operation.scope) {
                    Scope.REQUEST -> requestMetadata.requestHash
                    Scope.CLASS -> requestMetadata.classHash
                    Scope.ALL -> throw IllegalStateException("This should not happen")
                })
            }
        }

        val query = arrayOf(
                olderEntriesClause,
                requestClause
        ).filterNotNull()
                .joinToString(separator = " AND ")

        database.delete(
                TABLE_DEJA_VU,
                query,
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
    override fun <R : Any> get(requestMetadata: HashedRequestMetadata<R>): Serialised? {
        val projection = arrayOf(
                CACHE_DATE.columnName,
                EXPIRY_DATE.columnName,
                REQUEST.columnName,
                CLASS.columnName,
                SERIALISATION.columnName,
                DATA.columnName
        )

        val query = """
            SELECT ${projection.joinToString(", ")}
            FROM $TABLE_DEJA_VU
            WHERE ${REQUEST.columnName} = '${requestMetadata.requestHash}'
            LIMIT 1
            """

        return database.query(query).useAndLogError {
            with(it) {
                val simpleName = requestMetadata.responseClass.simpleName
                if (count != 0 && moveToNext()) {
                    logger.d(this, "Found a cached $simpleName")

                    val cacheDate = dateFactory(getLong(getColumnIndex(CACHE_DATE.columnName)))
                    val expiryDate = dateFactory(getLong(getColumnIndex(EXPIRY_DATE.columnName)))
                    val requestHash = getString(getColumnIndex(REQUEST.columnName))
                    val classHash = getString(getColumnIndex(CLASS.columnName))
                    val serialisation = getString(getColumnIndex(SERIALISATION.columnName))
                    val localData = getBlob(getColumnIndex(DATA.columnName))

                    Serialised(
                            requestHash,
                            classHash,
                            cacheDate,
                            expiryDate,
                            serialisation,
                            localData
                    )
                } else {
                    logger.d(this, "Found no cached $simpleName")
                    null
                }
            }
        }
    }

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE).
     *
     * @param requestMetadata the request's metadata
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    override fun <R : Any> forceInvalidation(token: RequestToken<*, R>): Boolean {
        val map = mapOf(EXPIRY_DATE.columnName to 0)
        val selection = "${REQUEST.columnName} = ?"

        val requestMetadata = token.instruction.requestMetadata
        val selectionArgs = arrayOf(requestMetadata.requestHash)

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
    override fun <R : Any> put(response: Response<R, Cache>) {
        val serialised = serialise(response)

        val cacheToken = response.cacheToken
        val requestMetadata = cacheToken.instruction.requestMetadata

        val values = HashMap<String, Any>()

        values[REQUEST.columnName] = requestMetadata.requestHash
        values[CLASS.columnName] = requestMetadata.classHash
        values[CACHE_DATE.columnName] = cacheToken.requestDate.time
        values[EXPIRY_DATE.columnName] = cacheToken.expiryDate!!.time
        values[SERIALISATION.columnName] = cacheToken.instruction.operation.serialisation
        values[DATA.columnName] = serialised

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

    private fun <T : Closeable?, R> T.useAndLogError(block: (T) -> R) =
            try {
                use(block)
            } catch (e: Exception) {
                logger.e(this@DatabasePersistenceManager, e, "Caught an IO exception")
                throw e
            }

}
