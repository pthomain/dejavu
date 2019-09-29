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

package dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.statistics.database

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.pthomain.android.boilerplate.core.utils.io.useAndLogError
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.PersistenceManager.Companion.getCacheStatus
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.database.CacheEntry
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.database.SqlOpenHelperCallback.Companion.COLUMNS.*
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.database.SqlOpenHelperCallback.Companion.TABLE_CACHE
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.statistics.BaseStatisticsCompiler
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.statistics.database.DatabaseStatisticsCompiler.CursorIteror
import java.util.*

//TODO JavaDoc
internal class DatabaseStatisticsCompiler(
        configuration: CacheConfiguration<*>,
        private val logger: Logger,
        private val dateFactory: (Long?) -> Date,
        private val database: SupportSQLiteDatabase
) : BaseStatisticsCompiler<Cursor, CursorIteror>(configuration) {

    private val projection = arrayOf(
            CLASS.columnName,
            IS_ENCRYPTED.columnName,
            IS_COMPRESSED.columnName,
            DATE.columnName,
            EXPIRY_DATE.columnName
    )

    private val query = """
                    SELECT ${projection.joinToString(", ")}
                    FROM $TABLE_CACHE
                    ORDER BY ${CLASS.columnName} ASC
                """

    override fun loadEntries() =
            database.query(query)
                    .useAndLogError(::CursorIteror, logger)

    override fun convert(entry: Cursor): CacheEntry {
        with(entry) {
            val responseClassName = getString(getColumnIndex(CLASS.columnName))
            val isEncrypted = getInt(getColumnIndex(IS_ENCRYPTED.columnName)) != 0
            val isCompressed = getInt(getColumnIndex(IS_COMPRESSED.columnName)) != 0
            val cacheDate = dateFactory(getLong(getColumnIndex(DATE.columnName)))
            val expiryDate = dateFactory(getLong(getColumnIndex(EXPIRY_DATE.columnName)))

            val status = getCacheStatus(expiryDate, dateFactory)

            return CacheEntry(
                    Class.forName(responseClassName),
                    status,
                    isEncrypted,
                    isCompressed,
                    cacheDate,
                    expiryDate
            )
        }
    }

    class CursorIteror(private val cursor: Cursor) : Iterator<Cursor>, Iterable<Cursor> {
        override fun iterator() = this
        override fun hasNext() = cursor.moveToNext()
        override fun next() = cursor
    }
}