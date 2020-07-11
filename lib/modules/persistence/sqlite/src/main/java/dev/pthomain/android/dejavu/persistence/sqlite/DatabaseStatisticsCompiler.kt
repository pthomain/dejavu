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

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.metadata.token.getCacheStatus
import dev.pthomain.android.dejavu.di.DateFactory
import dev.pthomain.android.dejavu.persistence.sqlite.SqlOpenHelperCallback.Companion.COLUMNS.*
import dev.pthomain.android.dejavu.persistence.sqlite.SqlOpenHelperCallback.Companion.TABLE_DEJA_VU
import dev.pthomain.android.dejavu.persistence.statistics.BaseStatisticsCompiler
import dev.pthomain.android.dejavu.persistence.statistics.CacheEntry
import java.io.Closeable

/**
 * Provides a concrete StatisticsCompiler implementation for SQLite database entries.
 *
 * @param configuration the global cache configuration
 * @param logger a Logger instance
 * @param dateFactory the factory converting timestamps to Dates
 * @param database the SQLite database containing the cache entries
 */
class DatabaseStatisticsCompiler internal constructor(
        private val logger: Logger,
        private val dateFactory: DateFactory,
        private val database: SupportSQLiteDatabase
) : BaseStatisticsCompiler<Cursor, CursorIterator>() {

    private val projection = arrayOf(
            CLASS.columnName,
            CACHE_DATE.columnName,
            EXPIRY_DATE.columnName
    )

    private val query = """
                    SELECT ${projection.joinToString(", ")}
                    FROM $TABLE_DEJA_VU
                    ORDER BY ${CLASS.columnName} ASC
                """

    /**
     * Returns an CursorIterator of the database entries.
     *
     * @return the CursorIterator
     */
    override fun loadEntries() =
            database.query(query).useAndLogError(::CursorIterator)

    /**
     * Converts a database Cursor to a CacheEntry.
     *
     * @param entry the current cursor
     * @return the converted entry
     */
    override fun convert(entry: Cursor) = with(entry) {
        val responseClassName = getString(getColumnIndex(CLASS.columnName))
        val cacheDate = dateFactory(getLong(getColumnIndex(CACHE_DATE.columnName)))
        val expiryDate = dateFactory(getLong(getColumnIndex(EXPIRY_DATE.columnName)))
        val status = dateFactory.getCacheStatus(expiryDate)

        CacheEntry(
                Class.forName(responseClassName),
                status,
                cacheDate,
                expiryDate
        )
    }

    private fun <T : Closeable?, R> T.useAndLogError(block: (T) -> R) =
            try {
                use(block)
            } catch (e: Exception) {
                logger.e(this@DatabaseStatisticsCompiler, e, "Caught an IO exception")
                throw e
            }
}