package uk.co.glass_software.android.cache_interceptor.interceptors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.google.gson.Gson;

import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor;
import uk.co.glass_software.android.cache_interceptor.response.base.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;
import uk.co.glass_software.android.cache_interceptor.utils.SimpleLogger;

public class RxCacheInterceptorBuilder<E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> {
    
    private Logger logger;
    private Function<Throwable, E> errorFactory;
    private String databaseName;
    private Gson gson;
    private boolean compressData = true;
    private boolean encryptData = false;
    
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
    
    public RxCacheInterceptorBuilder<E, R> compress(boolean compressData) {
        this.compressData = compressData;
        return this;
    }
    
    public RxCacheInterceptorBuilder<E, R> encrypt(boolean encryptData) {
        this.encryptData = encryptData;
        return this;
    }
    
    @SuppressLint("RestrictedApi")
    public RxCacheInterceptor.Factory<E, R> build(Context context) {
        return build(context, null);
    }
    
    @RestrictTo(RestrictTo.Scope.TESTS)
    RxCacheInterceptor.Factory<E, R> build(Context context,
                                           @Nullable DependencyHolder holder) {
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
                .build(context.getApplicationContext(), compressData, encryptData);
        
        
        if (holder != null) {
            holder.gson = gson;
            holder.errorFactory = this.errorFactory;
            holder.errorInterceptorFactory = errorInterceptorFactory;
            holder.cacheInterceptorFactory = cacheInterceptorFactory;
        }
        
        return new RxCacheInterceptor.Factory<>(errorInterceptorFactory,
                                                cacheInterceptorFactory,
                                                logger
        );
    }
    
    @RestrictTo(RestrictTo.Scope.TESTS)
    class DependencyHolder {
        Function<Throwable, E> errorFactory;
        Gson gson;
        ErrorInterceptor.Factory<E> errorInterceptorFactory;
        CacheInterceptor.Factory<E> cacheInterceptorFactory;
    }
}
