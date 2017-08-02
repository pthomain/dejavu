package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.util.Date;
import java.util.Map;

import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor;
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

public class CacheInterceptorBuilder<E extends Exception> {
    
    private final static String DATABASE_NAME = "http_cache.db";
    private final Function<Throwable, E> errorFactory;
    
    private long timeToLiveInMinutes = 5;
    private String databaseName;
    private Logger logger;
    private Gson gson;
    
    CacheInterceptorBuilder(Function<Throwable, E> errorFactory) {
        this.errorFactory = errorFactory;
    }
    
    public CacheInterceptorBuilder<E> gson(@NonNull Gson gson) {
        this.gson = gson;
        return this;
    }
    
    public CacheInterceptorBuilder<E> databaseName(@NonNull String databaseName) {
        this.databaseName = databaseName;
        return this;
    }
    
    public CacheInterceptorBuilder<E> ttlInMinutes(@NonNull Integer timeToLiveInMinutes) {
        if (timeToLiveInMinutes >= 0) {
            this.timeToLiveInMinutes = timeToLiveInMinutes;
        }
        else {
            throw new IllegalArgumentException("timeToLiveInMinutes should be positive");
        }
        return this;
    }
    
    public CacheInterceptorBuilder<E> logger(@NonNull Logger logger) {
        this.logger = logger;
        return this;
    }
    
    //Used for unit testing
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
    
    public RetrofitCacheAdapterFactory<E> buildAdapter(Context context) {
        CacheInterceptor.Factory<E> cacheInterceptorFactory = build(context);
        
        ErrorInterceptor.Factory<E> errorInterceptorFactory = new ErrorInterceptor.Factory<>(
                errorFactory,
                getLogger()
        );
        
        return new RetrofitCacheAdapterFactory<>(
                errorInterceptorFactory,
                cacheInterceptorFactory
        );
    }
    
    public CacheInterceptor.Factory<E> build(@NonNull Context context) {
        if (gson == null) {
            gson = new Gson();
        }
        
        Logger logger = getLogger();
        
        SerialisationManager serialisationManager = new SerialisationManager(
                logger,
                gson
        );
        
        SQLiteDatabase database = new SqlOpenHelper(context,
                                                    databaseName == null ? DATABASE_NAME : databaseName
        ).getWritableDatabase();
        
        DatabaseManager databaseManager = new DatabaseManager(
                database,
                serialisationManager,
                logger,
                Date::new,
                this::mapToContentValues
        );
        
        CacheManager cacheManager = new CacheManager(
                databaseManager,
                Date::new,
                gson,
                logger,
                timeToLiveInMinutes
        );
        
        return new CacheInterceptor.Factory<>(
                cacheManager,
                true,
                logger
        );
    }
    
    private Logger getLogger() {
        return logger != null ? logger
                              : new Logger() {
                                  @Override
                                  public void e(Object caller, Throwable t, String message) {}
            
                                  @Override
                                  public void e(Object caller, String message) {}
            
                                  @Override
                                  public void d(Object caller, String message) {}
                              };
    }
}
