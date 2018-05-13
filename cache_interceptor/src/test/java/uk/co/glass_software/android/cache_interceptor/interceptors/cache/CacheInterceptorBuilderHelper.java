package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.google.gson.Gson;

import java.util.Date;

import uk.co.glass_software.android.cache_interceptor.base.network.model.TestResponse;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

public class CacheInterceptorBuilderHelper {
    
    public final Gson gson;
    public final SerialisationManager serialisationManager;
    public final SQLiteDatabase database;
    public final Function<Long, Date> dateFactory;
    public final DatabaseManager databaseManager;
    public final CacheManager cacheManager;
    
    public CacheInterceptorBuilderHelper(Context context) {
        CacheInterceptorBuilder.Holder holder = new CacheInterceptorBuilder.Holder();
        new CacheInterceptorBuilder<ApiError>().build(
                context.getApplicationContext(),
                true,
                false,
                holder
        );
        
        gson = holder.gson;
        serialisationManager = holder.serialisationManager;
        database = holder.database;
        dateFactory = holder.dateFactory;
        databaseManager = holder.databaseManager;
        cacheManager = holder.cacheManager;
    }
    
}