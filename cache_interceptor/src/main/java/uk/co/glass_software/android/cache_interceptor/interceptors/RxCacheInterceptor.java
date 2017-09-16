package uk.co.glass_software.android.cache_interceptor.interceptors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.google.gson.Gson;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor;
import uk.co.glass_software.android.cache_interceptor.response.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

public class RxCacheInterceptor<E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>>
        implements ObservableTransformer<R, R> {
    
    @NonNull
    private final Class<R> responseClass;
    
    @NonNull
    private final String url;
    
    @NonNull
    private final String[] params;
    
    @NonNull
    private final String body;
    
    @Nullable
    private final Logger logger;
    
    @NonNull
    private final ErrorInterceptor.Factory<E> errorInterceptorFactory;
    
    @NonNull
    private final CacheInterceptor.Factory<E> cacheInterceptorFactory;
    
    @RestrictTo(RestrictTo.Scope.TESTS)
    RxCacheInterceptor(@NonNull Class<R> responseClass,
                       @NonNull String url,
                       @NonNull String[] params,
                       @NonNull String body,
                       @Nullable Logger logger,
                       @NonNull ErrorInterceptor.Factory<E> errorInterceptorFactory,
                       @NonNull CacheInterceptor.Factory<E> cacheInterceptorFactory) {
        this.responseClass = responseClass;
        this.url = url;
        this.params = params;
        this.body = body;
        this.logger = logger;
        this.errorInterceptorFactory = errorInterceptorFactory;
        this.cacheInterceptorFactory = cacheInterceptorFactory;
    }
    
    @Override
    public ObservableSource<R> apply(Observable<R> observable) {
        CacheToken<R> cacheToken;
        try {
            R response = responseClass.newInstance();
            cacheToken = response.getCacheToken(responseClass,
                                                url,
                                                params,
                                                body
            );
        }
        catch (Exception e) {
            logger.e(this,
                     e,
                     "Could not instantiate response of type: "
                     + responseClass.getName()
                     + "; not caching it"
            );
            cacheToken = CacheToken.doNotCache(responseClass);
        }
        
        ResponseMetadata<R, E> metadata = ResponseMetadata.create(cacheToken, null);
        
        return observable
                .compose(errorInterceptorFactory.create(metadata))
                .compose(cacheInterceptorFactory.create(cacheToken));
    }
    
    public static <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> RxCacheInterceptorBuilder<E, R> builder() {
        return new RxCacheInterceptorBuilder<>();
    }
    
    public static <R extends ResponseMetadata.Holder<R, ApiError>> RxCacheInterceptor.Factory<ApiError, R> buildDefault(Context context) {
        return RxCacheInterceptor.<ApiError, R>builder()
                .gson(new Gson())
                .errorFactory(ApiError::new)
                .build(context);
    }
    
    public static class Factory<E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> {
        
        @NonNull
        private final ErrorInterceptor.Factory<E> errorInterceptorFactory;
        
        @NonNull
        private final CacheInterceptor.Factory<E> cacheInterceptorFactory;
        
        @Nullable
        private final Logger logger;
        
        Factory(@NonNull ErrorInterceptor.Factory<E> errorInterceptorFactory,
                @NonNull CacheInterceptor.Factory<E> cacheInterceptorFactory,
                @Nullable Logger logger) {
            this.errorInterceptorFactory = errorInterceptorFactory;
            this.cacheInterceptorFactory = cacheInterceptorFactory;
            this.logger = logger;
        }
        
        @SuppressLint("RestrictedApi")
        public RxCacheInterceptor<E, R> create(@NonNull Class<R> responseClass,
                                               @NonNull String url,
                                               @NonNull String[] params,
                                               @NonNull String body) {
            return new RxCacheInterceptor<>(responseClass,
                                            url,
                                            params,
                                            body,
                                            logger,
                                            errorInterceptorFactory,
                                            cacheInterceptorFactory
            );
        }
    }
}
