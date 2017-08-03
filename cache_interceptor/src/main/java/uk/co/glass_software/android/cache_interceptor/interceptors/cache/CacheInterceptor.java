package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.content.Context;

import java.util.Date;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.retrofit.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;
import uk.co.glass_software.android.cache_interceptor.utils.SimpleLogger;

import static uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken.Status.DO_NOT_CACHE;

public class CacheInterceptor<E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>>
        implements ObservableTransformer<R, R> {
    
    private final CacheManager cacheManager;
    private final boolean isCacheEnabled;
    private final Logger logger;
    private final CacheToken<R> cacheToken;
    
    CacheInterceptor(CacheManager cacheManager,
                     boolean isCacheEnabled,
                     Logger logger,
                     CacheToken<R> cacheToken) {
        this.cacheManager = cacheManager;
        this.logger = logger;
        this.cacheToken = cacheToken;
        this.isCacheEnabled = isCacheEnabled;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public ObservableSource<R> apply(Observable<R> upstream) {
        Observable<R> observable = isCacheEnabled && cacheToken.getStatus() != DO_NOT_CACHE
                                   ? cache(upstream, cacheToken)
                                   : doNotCache(upstream, cacheToken);
        
        return observable.doOnNext(response -> {
            ResponseMetadata<R, E> metadata = response.getMetadata();
            if (metadata.getCacheToken().getStatus() == DO_NOT_CACHE) {
                metadata.setCacheToken(CacheToken.notCached(metadata.getCacheToken(),
                                                            upstream,
                                                            new Date()
                ));
            }
            logger.d(this, "Returning: " + cacheToken.toString());
        });
    }
    
    @SuppressWarnings("unchecked")
    private Observable<R> doNotCache(Observable<R> upstream,
                                     CacheToken<R> cacheToken) {
        return upstream.doOnNext(response -> response.getMetadata()
                                                     .setCacheToken(CacheToken.notCached(cacheToken,
                                                                                         upstream,
                                                                                         new Date()
                                                     )));
    }
    
    private Observable<R> cache(Observable<R> upstream,
                                CacheToken<R> cacheToken) {
        return cacheManager.getCachedResponse(upstream, cacheToken);
    }
    
    public static class Factory<E extends Exception & Function<E, Boolean>> {
        
        private final CacheManager cacheManager;
        private final boolean isCacheEnabled;
        private final Logger logger;
        
        Factory(CacheManager cacheManager,
                boolean isCacheEnabled,
                Logger logger) {
            this.cacheManager = cacheManager;
            this.isCacheEnabled = isCacheEnabled;
            this.logger = logger;
        }
        
        public <R extends ResponseMetadata.Holder<R, E>> CacheInterceptor<E, R> create(CacheToken<R> cacheToken) {
            return new CacheInterceptor<>(cacheManager,
                                          isCacheEnabled,
                                          logger,
                                          cacheToken
            );
        }
    }
    
    public static <E extends Exception & Function<E, Boolean>> CacheInterceptorBuilder<E> builder(Function<Throwable, E> errorFactory) {
        return new CacheInterceptorBuilder<>(errorFactory);
    }
    
    public static CacheInterceptorBuilder<ApiError> builder() {
        return new CacheInterceptorBuilder<>(ApiError::new);
    }
    
    public static RetrofitCacheAdapterFactory<ApiError> buildSimpleAdapterFactory(Context context) {
        return new CacheInterceptorBuilder<>(ApiError::new)
                .logger(new SimpleLogger(context))
                .buildAdapter(context);
    }
}