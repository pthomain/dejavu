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

package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database

import android.content.Context
import io.requery.android.database.sqlite.SQLiteCursor
import io.requery.android.database.sqlite.SQLiteDatabase
import io.requery.android.database.sqlite.SQLiteOpenHelper
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database.SqlOpenHelper.Companion.COLUMNS.*

internal class SqlOpenHelper(context: Context,
                             databaseName: String)
    : SQLiteOpenHelper(
        context,
        databaseName,
        cursorFactory,
        DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s (%s)",
                TABLE_CACHE,
                values().joinToString(separator = ", ") { it.columnName + " " + it.type }
        ))

        addIndex(db, TOKEN.columnName)
        addIndex(db, EXPIRY_DATE.columnName)
    }

    private fun addIndex(db: SQLiteDatabase,
                         columnName: String) {
        db.execSQL(String.format("CREATE INDEX IF NOT EXISTS %s_index ON %s(%s)",
                columnName,
                TABLE_CACHE,
                columnName
        ))
    }

    override fun onDowngrade(db: SQLiteDatabase,
                             oldVersion: Int,
                             newVersion: Int) = onUpgrade(db, oldVersion, newVersion)

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase,
                           oldVersion: Int,
                           newVersion: Int) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_CACHE")
        onCreate(sqLiteDatabase)
    }

    companion object {
        private const val DATABASE_VERSION = 2

        const val TABLE_CACHE = "rx_cache"

        enum class COLUMNS(val columnName: String,
                           val type: String) {
            TOKEN("token", "TEXT UNIQUE"),
            DATE("cache_date", "INTEGER"),
            EXPIRY_DATE("expiry_date", "INTEGER"),
            DATA("data", "NONE"),
            CLASS("class", "TEXT"),
            IS_ENCRYPTED("is_encrypted", "INTEGER"),
            IS_COMPRESSED("is_compressed", "INTEGER");
        }

        private val cursorFactory = SQLiteDatabase.CursorFactory { _, driver, editTable, query ->
            SQLiteCursor(driver, editTable, query)
        }
    }
}
