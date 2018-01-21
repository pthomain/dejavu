package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.annotation.SuppressLint;
import android.support.annotation.RestrictTo;

import java.util.Date;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import uk.co.glass_software.android.cache_interceptor.response.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

import static uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken.Status.DO_NOT_CACHE;

public class CacheInterceptor<E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>>
        implements ObservableTransformer<R, R> {
    
    private final CacheManager cacheManager;
    private final boolean isCacheEnabled;
    private final Logger logger;
    private final Function<E, Boolean> isNetworkError;
    private final CacheToken<R> cacheToken;
    
    @RestrictTo(RestrictTo.Scope.TESTS)
    CacheInterceptor(CacheManager cacheManager,
                     boolean isCacheEnabled,
                     Logger logger,
                     Function<E, Boolean> isNetworkError,
                     CacheToken<R> cacheToken) {
        this.cacheManager = cacheManager;
        this.logger = logger;
        this.isNetworkError = isNetworkError;
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
        return cacheManager.getCachedResponse(upstream, cacheToken, isNetworkError);
    }
    
    public static <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> CacheInterceptorBuilder<E, R> builder() {
        return new CacheInterceptorBuilder<>();
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
        
        @SuppressLint("RestrictedApi")
        public <R extends ResponseMetadata.Holder<R, E>> CacheInterceptor<E, R> create(CacheToken<R> cacheToken,
                                                                                       Function<E, Boolean> isNetworkError) {
            return new CacheInterceptor<>(cacheManager,
                                          isCacheEnabled,
                                          logger,
                                          isNetworkError,
                                          cacheToken
            );
        }
    
        public void clearOlderEntries() {
            cacheManager.clearOlderEntries();
        }
    
        public void flushCache() {
            cacheManager.flushCache();
        }
    }
}