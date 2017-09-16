package uk.co.glass_software.android.cache_interceptor.interceptors;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor;
import uk.co.glass_software.android.cache_interceptor.response.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;
import uk.co.glass_software.android.cache_interceptor.utils.SimpleLogger;

public class RxCacheInterceptorBuilder<E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> {
    
    private Logger logger;
    private Function<Throwable, E> errorFactory;
    private String databaseName;
    private int timeToLiveInMinutes = 5;
    private Gson gson;
    
    RxCacheInterceptorBuilder() {
    }
    
    public RxCacheInterceptorBuilder<E, R> errorFactory(Function<Throwable, E> errorFactory) {
        this.errorFactory = errorFactory;
        return this;
    }
    
    public RxCacheInterceptorBuilder<E, R> noLog() {
        return logger(new Logger() {
            @Override
            public void e(Object caller, Throwable t, String message) {}
            
            @Override
            public void e(Object caller, String message) {}
            
            @Override
            public void d(Object caller, String message) {}
        });
    }
    
    public RxCacheInterceptorBuilder<E, R> logger(Logger logger) {
        this.logger = logger;
        return this;
    }
    
    public RxCacheInterceptorBuilder<E, R> gson(@NonNull Gson gson) {
        this.gson = gson;
        return this;
    }
    
    public RxCacheInterceptorBuilder<E, R> databaseName(@NonNull String databaseName) {
        this.databaseName = databaseName;
        return this;
    }
    
    public RxCacheInterceptorBuilder<E, R> ttlInMinutes(@NonNull Integer timeToLiveInMinutes) {
        if (timeToLiveInMinutes >= 0) {
            this.timeToLiveInMinutes = timeToLiveInMinutes;
        }
        else {
            throw new IllegalArgumentException("timeToLiveInMinutes should be positive");
        }
        return this;
    }
    
    public RxCacheInterceptor.Factory<E, R> build(Context context) {
        if (logger == null) {
            logger = new SimpleLogger(context.getApplicationContext());
        }
        
        if (errorFactory == null) {
            throw new IllegalStateException("Please provide an error factory");
        }
        
        ErrorInterceptor.Factory<E> errorInterceptorFactory = new ErrorInterceptor.Factory<>(
                errorFactory,
                logger
        );
        CacheInterceptor.Factory<E> cacheInterceptorFactory = CacheInterceptor.<E, R>builder()
                .logger(logger)
                .databaseName(databaseName)
                .gson(gson)
                .ttlInMinutes(timeToLiveInMinutes)
                .build(context.getApplicationContext());
        
        return new RxCacheInterceptor.Factory<>(errorInterceptorFactory,
                                                cacheInterceptorFactory,
                                                logger
        );
    }
}
