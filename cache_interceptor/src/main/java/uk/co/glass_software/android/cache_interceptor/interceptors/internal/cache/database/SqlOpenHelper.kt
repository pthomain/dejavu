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
        db.execSQL(String.format("CREATE TABLE %s (%s)",
                TABLE_CACHE,
                values().joinToString(separator = ", ") { it.columnName + " " + it.type }
        ))

        addIndex(db, TOKEN.columnName)
        addIndex(db, EXPIRY_DATE.columnName)
    }

    private fun addIndex(db: SQLiteDatabase,
                         columnName: String) {
        db.execSQL(String.format("CREATE INDEX %s_index ON %s(%s)",
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

        const val TABLE_CACHE = "http_cache"

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
