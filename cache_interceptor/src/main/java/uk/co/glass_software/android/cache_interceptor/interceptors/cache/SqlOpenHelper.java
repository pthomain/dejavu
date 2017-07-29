package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.content.Context;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

class SqlOpenHelper extends SQLiteOpenHelper {
    
    private final static int DATABASE_VERSION = 1;
    
    final static String TABLE_CACHE = "http_cache";
    final static String COLUMN_CACHE_TOKEN = "token";
    final static String COLUMN_CACHE_DATE = "cache_date";
    final static String COLUMN_CACHE_EXPIRY_DATE = "expiry_date";
    final static String COLUMN_CACHE_DATA = "data";
    
    SqlOpenHelper(Context context,
                  String databaseName) {
        super(context,
              databaseName,
              getCursorFactory(),
              DATABASE_VERSION
        );
    }
    
    @NonNull
    private static SQLiteDatabase.CursorFactory getCursorFactory() {
        return (db, driver, editTable, query) -> new SQLiteCursor(driver, editTable, query);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE %s (%s TEXT UNIQUE, %s INTEGER, %s INTEGER, %s NONE)",
                                 TABLE_CACHE,
                                 COLUMN_CACHE_TOKEN,
                                 COLUMN_CACHE_DATE,
                                 COLUMN_CACHE_EXPIRY_DATE,
                                 COLUMN_CACHE_DATA
        ));
        db.execSQL(String.format("CREATE INDEX %s_index ON %s(%s)",
                                 COLUMN_CACHE_EXPIRY_DATE,
                                 TABLE_CACHE,
                                 COLUMN_CACHE_EXPIRY_DATE
        ));
        db.execSQL(String.format("CREATE INDEX %s_index ON %s(%s)",
                                 COLUMN_CACHE_TOKEN,
                                 TABLE_CACHE,
                                 COLUMN_CACHE_TOKEN
        ));
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase,
                          int oldVersion,
                          int newVersion) {
    }
}
