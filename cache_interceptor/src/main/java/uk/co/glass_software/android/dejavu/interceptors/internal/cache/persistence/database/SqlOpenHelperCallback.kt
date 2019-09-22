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

package uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence.database.SqlOpenHelperCallback.Companion.COLUMNS.*

/**
 * Callback handling the lifecycle of the database.
 *
 * @param databaseVersion the current database version
 */
internal class SqlOpenHelperCallback(databaseVersion: Int)
    : SupportSQLiteOpenHelper.Callback(databaseVersion) {

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL(String.format("CREATE TABLE IF NOT EXISTS %s (%s)",
                TABLE_CACHE,
                values().joinToString(separator = ", ") { it.columnName + " " + it.type }
        ))

        addIndex(db, TOKEN.columnName)
        addIndex(db, EXPIRY_DATE.columnName)
    }

    /**
     * Adds an index on the given column
     *
     * @param db The database.
     * @param columnName the name of the column on which to define an index
     */
    private fun addIndex(db: SupportSQLiteDatabase,
                         columnName: String) {
        db.execSQL(String.format("CREATE INDEX IF NOT EXISTS %s_index ON %s(%s)",
                columnName,
                TABLE_CACHE,
                columnName
        ))
    }

    /**
     * Called when the database needs to be downgraded. This is strictly similar to
     * {@link #onUpgrade} method, but is called whenever current version is newer than requested
     * one.
     * However, this method is not abstract, so it is not mandatory for a customer to
     * implement it. If not overridden, default implementation will reject downgrade and
     * throws SQLiteException
     *
     * <p>
     * This method executes within a transaction.  If an exception is thrown, all changes
     * will automatically be rolled back.
     * </p>
     *
     * @param db         The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    override fun onDowngrade(db: SupportSQLiteDatabase,
                             oldVersion: Int,
                             newVersion: Int) = onUpgrade(db, oldVersion, newVersion)

    /**
     * Called when the database needs to be upgraded. The implementation
     * should use this method to drop tables, add tables, or do anything else it
     * needs to upgrade to the new schema version.
     *
     * <p>
     * The SQLite ALTER TABLE documentation can be found
     * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
     * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
     * you can use ALTER TABLE to rename the old table, then create the new table and then
     * populate the new table with the contents of the old table.
     * </p><p>
     * This method executes within a transaction.  If an exception is thrown, all changes
     * will automatically be rolled back.
     * </p>
     *
     * @param db         The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    override fun onUpgrade(sqLiteDatabase: SupportSQLiteDatabase,
                           oldVersion: Int,
                           newVersion: Int) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS $TABLE_CACHE")
        onCreate(sqLiteDatabase)
    }

    companion object {
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

    }
}
