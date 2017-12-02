package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import com.google.gson.Gson;

import java.util.Date;
import java.util.Map;

import uk.co.glass_software.android.cache_interceptor.response.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;
import uk.co.glass_software.android.shared_preferences.StoreEntryFactory;

public class CacheInterceptorBuilder<E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> {
    
    private final static String DATABASE_NAME = "http_cache.db";
    
    private String databaseName;
    private Logger logger;
    private Gson gson;
    
    CacheInterceptorBuilder() {
    }
    
    public CacheInterceptorBuilder<E, R> gson(@NonNull Gson gson) {
        this.gson = gson;
        return this;
    }
    
    public CacheInterceptorBuilder<E, R> databaseName(@NonNull String databaseName) {
        this.databaseName = databaseName;
        return this;
    }
    
    public CacheInterceptorBuilder<E, R> logger(@NonNull Logger logger) {
        this.logger = logger;
        return this;
    }
    
    //Used for unit testing
    @VisibleForTesting
    ContentValues mapToContentValues(Map<String, ?> map) {
        ContentValues values = new ContentValues();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            
            if (value instanceof Boolean) {
                values.put(entry.getKey(), (Boolean) value);
            }
            else if (value instanceof Float) {
                values.put(entry.getKey(), (Float) value);
            }
            else if (value instanceof Double) {
                values.put(entry.getKey(), (Double) value);
            }
            else if (value instanceof Long) {
                values.put(entry.getKey(), (Long) value);
            }
            else if (value instanceof Integer) {
                values.put(entry.getKey(), (Integer) value);
            }
            else if (value instanceof Byte) {
                values.put(entry.getKey(), (Byte) value);
            }
            else if (value instanceof byte[]) {
                values.put(entry.getKey(), (byte[]) value);
            }
            else if (value instanceof Short) {
                values.put(entry.getKey(), (Short) value);
            }
            else if (value instanceof String) {
                values.put(entry.getKey(), (String) value);
            }
        }
        return values;
    }
    
    @SuppressLint("RestrictedApi")
    public CacheInterceptor.Factory<E> build(@NonNull Context context) {
        return build(context, false, false);
    }
    
    @SuppressLint("RestrictedApi")
    public CacheInterceptor.Factory<E> build(@NonNull Context context,
                                             boolean compressData,
                                             boolean encryptData) {
        return build(context, compressData, encryptData, null);
    }
    
    @RestrictTo(RestrictTo.Scope.TESTS)
    CacheInterceptor.Factory<E> build(@NonNull Context context,
                                      boolean compressData,
                                      boolean encryptData,
                                      @Nullable Holder holder) {
        if (gson == null) {
            gson = new Gson();
        }
        
        Logger logger = this.logger != null ? this.logger
                                            : new Logger() {
                                                @Override
                                                public void e(Object caller, Throwable t, String message) {}
            
                                                @Override
                                                public void e(Object caller, String message) {}
            
                                                @Override
                                                public void d(Object caller, String message) {}
                                            };
        
        StoreEntryFactory storeEntryFactory = new StoreEntryFactory(context);
        SerialisationManager serialisationManager = new SerialisationManager(
                logger,
                storeEntryFactory,
                encryptData,
                compressData,
                gson
        );
        
        SQLiteDatabase database = new SqlOpenHelper(
                context.getApplicationContext(),
                databaseName == null ? DATABASE_NAME : databaseName
        ).getWritableDatabase();
        
        Function<Long, Date> dateFactory = timestamp -> timestamp == null ? new Date() : new Date(timestamp);
        
        
        DatabaseManager databaseManager = new DatabaseManager(
                database,
                serialisationManager,
                logger,
                dateFactory,
                this::mapToContentValues
        );
        
        CacheManager cacheManager = new CacheManager(
                databaseManager,
                dateFactory,
                gson,
                logger
        );
        
        if (holder != null) {
            holder.gson = gson;
            holder.serialisationManager = serialisationManager;
            holder.database = database;
            holder.dateFactory = dateFactory;
            holder.databaseManager = databaseManager;
            holder.cacheManager = cacheManager;
        }
        
        return new CacheInterceptor.Factory<>(
                cacheManager,
                true,
                logger
        );
    }
    
    @RestrictTo(RestrictTo.Scope.TESTS)
    static class Holder {
        Gson gson;
        SerialisationManager serialisationManager;
        SQLiteDatabase database;
        Function<Long, Date> dateFactory;
        DatabaseManager databaseManager;
        CacheManager cacheManager;
    }
}
