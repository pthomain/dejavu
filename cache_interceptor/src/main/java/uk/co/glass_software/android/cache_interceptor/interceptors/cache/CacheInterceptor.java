package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import java.io.IOException;
import java.util.Date;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import uk.co.glass_software.android.utils.Function;
import uk.co.glass_software.android.utils.Logger;

public class CacheInterceptor<E extends Exception, R extends CacheToken.Holder<R, E>>
        implements ObservableTransformer<R, R> {
    
    private final CacheManager cacheManager;
    private final boolean isCacheEnabled;
    private final Logger logger;
    private final Class<R> responseClass;
    private final Function<E, Boolean> isNetworkError;
    private final Function<Pair<Class<R>, String[]>, CacheToken<R>> tokenProvider;
    private final String[] params;
    
    CacheInterceptor(CacheManager cacheManager,
                     boolean isCacheEnabled,
                     Logger logger,
                     Class<R> responseClass,
                     Function<E, Boolean> isNetworkError,
                     Function<Pair<Class<R>, String[]>, CacheToken<R>> tokenProvider,
                     String[] params) {
        this.cacheManager = cacheManager;
        this.logger = logger;
        this.responseClass = responseClass;
        this.isNetworkError = isNetworkError;
        this.tokenProvider = tokenProvider;
        this.params = params;
        this.isCacheEnabled = isCacheEnabled;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public ObservableSource<R> apply(Observable<R> upstream) {
        Pair<Class<R>, String[]> paramsPair = new Pair<>(responseClass, params);
        CacheToken<R> cacheToken = tokenProvider.get(paramsPair);
        
        Observable<R> observable = isCacheEnabled && cacheToken.getStatus() != CacheToken.Status.DO_NOT_CACHE
                                   ? cache(upstream, cacheToken)
                                   : doNotCache(upstream, cacheToken);
        
        return observable.doOnNext(response -> {
            if (response.getCacheToken().getStatus() == CacheToken.Status.DO_NOT_CACHE) {
                response.setCacheToken(CacheToken.notCached(response.getCacheToken(), upstream, new Date()));
            }
            logger.d(this, "Returning: " + cacheToken.toString());
        });
    }
    
    @SuppressWarnings("unchecked")
    private Observable<R> doNotCache(Observable<R> upstream,
                                     CacheToken<R> cacheToken) {
        return upstream.doOnNext(response -> response.setCacheToken(CacheToken.notCached(cacheToken, upstream, new Date())));
    }
    
    private Observable<R> cache(Observable<R> upstream,
                                CacheToken<R> cacheToken) {
        return cacheManager.getCachedResponse(upstream, cacheToken, isNetworkError);
    }
    
    public static class Factory<E extends Exception> {
        
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
        
        public <R extends CacheToken.Holder<R, E>> CacheInterceptor<E, R> create(Class<R> responseClass,
                                                                                 String apiUrl,
                                                                                 Function<E, Boolean> isNetworkError) {
            return create(responseClass, apiUrl, null, isNetworkError);
        }
        
        public <R extends CacheToken.Holder<R, E>> CacheInterceptor<E, R> create(Class<R> responseClass,
                                                                                 String apiUrl,
                                                                                 @Nullable String[] params,
                                                                                 Function<E, Boolean> isNetworkError) {
            return create(responseClass,
                          pair -> CacheToken.newRequest(responseClass, apiUrl, params == null ? new String[0] : params),
                          isNetworkError,
                          null
            );
        }
        
        public <R extends CacheToken.Holder<R, E>> CacheInterceptor<E, R> create(Class<R> responseClass,
                                                                                 Function<Class<R>, CacheToken<R>> tokenProvider,
                                                                                 Function<E, Boolean> isNetworkError) {
            return create(responseClass,
                          pair -> tokenProvider.get(pair.first),
                          isNetworkError,
                          null
            );
        }
        
        public <R extends CacheToken.Holder<R, E>> CacheInterceptor<E, R> create(Class<R> responseClass,
                                                                                 Function<Pair<Class<R>, String[]>, CacheToken<R>> tokenProvider,
                                                                                 Function<E, Boolean> isNetworkError,
                                                                                 @Nullable String[] params) {
            return new CacheInterceptor<>(cacheManager,
                                          isCacheEnabled,
                                          logger,
                                          responseClass,
                                          isNetworkError,
                                          tokenProvider,
                                          params
            );
        }
    }
    
    public static <E extends Exception> CacheInterceptorBuilder<E> builder(Function<Throwable, E> errorFactory,
                                                                           Class<E> errorClass,
                                                                           Function<E, Boolean> isNetworkError) {
        return new CacheInterceptorBuilder<>(errorFactory,
                                             errorClass,
                                             isNetworkError
        );
    }
    
    public static CacheInterceptorBuilder<Exception> builder() {
        return new CacheInterceptorBuilder<>(throwable -> (Exception) throwable,
                                             Exception.class,
                                             throwable -> throwable instanceof IOException
        );
    }
}