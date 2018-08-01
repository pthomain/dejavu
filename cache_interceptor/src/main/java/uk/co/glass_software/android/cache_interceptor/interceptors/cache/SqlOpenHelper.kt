package uk.co.glass_software.android.cache_interceptor.interceptors.cache

import android.content.Context
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class SqlOpenHelper(context: Context,
                             databaseName: String)
    : SQLiteOpenHelper(context,
        databaseName,
        cursorFactory,
        DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(String.format("CREATE TABLE %s (%s TEXT UNIQUE, %s INTEGER, %s INTEGER, %s NONE)",
                TABLE_CACHE,
                COLUMN_CACHE_TOKEN,
                COLUMN_CACHE_DATE,
                COLUMN_CACHE_EXPIRY_DATE,
                COLUMN_CACHE_DATA
        ))
        db.execSQL(String.format("CREATE INDEX %s_index ON %s(%s)",
                COLUMN_CACHE_EXPIRY_DATE,
                TABLE_CACHE,
                COLUMN_CACHE_EXPIRY_DATE
        ))
        db.execSQL(String.format("CREATE INDEX %s_index ON %s(%s)",
                COLUMN_CACHE_TOKEN,
                TABLE_CACHE,
                COLUMN_CACHE_TOKEN
        ))
    }

    override fun onUpgrade(sqLiteDatabase: SQLiteDatabase,
                           oldVersion: Int,
                           newVersion: Int) = Unit

    companion object {
        private const val DATABASE_VERSION = 1

        const val TABLE_CACHE = "http_cache"
        const val COLUMN_CACHE_TOKEN = "token"
        const val COLUMN_CACHE_DATE = "cache_date"
        const val COLUMN_CACHE_EXPIRY_DATE = "expiry_date"
        const val COLUMN_CACHE_DATA = "data"

        private val cursorFactory = SQLiteDatabase.CursorFactory { _, driver, editTable, query ->
            SQLiteCursor(driver, editTable, query)
        }
    }
}
