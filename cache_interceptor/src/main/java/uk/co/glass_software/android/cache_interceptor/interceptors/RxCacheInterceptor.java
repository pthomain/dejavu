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
    
    @Nullable
    private final String body;
    
    @NonNull
    private final Logger logger;
    
    @NonNull
    private final ErrorInterceptor.Factory<E> errorInterceptorFactory;
    
    @NonNull
    private final CacheInterceptor.Factory<E> cacheInterceptorFactory;
    
    @RestrictTo(RestrictTo.Scope.TESTS)
    RxCacheInterceptor(@NonNull Class<R> responseClass,
                       @NonNull String url,
                       @Nullable String body,
                       @NonNull Logger logger,
                       @NonNull ErrorInterceptor.Factory<E> errorInterceptorFactory,
                       @NonNull CacheInterceptor.Factory<E> cacheInterceptorFactory) {
        this.responseClass = responseClass;
        this.url = url;
        this.body = body;
        this.logger = logger;
        this.errorInterceptorFactory = errorInterceptorFactory;
        this.cacheInterceptorFactory = cacheInterceptorFactory;
    }
    
    @Override
    public ObservableSource<R> apply(Observable<R> observable) {
        float ttlInMinutes;
        boolean isRefresh;
        try {
            R response = responseClass.newInstance();
            ttlInMinutes = response.getTtlInMinutes();
            isRefresh = response.isRefresh();
        }
        catch (Exception e) {
            logger.e(this,
                     e,
                     "Could not instantiate response of type: "
                     + responseClass.getName()
                     + "; using default TTL"
            );
            ttlInMinutes = ResponseMetadata.Holder.DEFAULT_TTL_IN_MINUTES;
            isRefresh = false;
        }
        
        CacheToken<R> cacheToken = isRefresh ? CacheToken.refresh(responseClass,
                                                                  url,
                                                                  body,
                                                                  ttlInMinutes
        )
                                             : CacheToken.newRequest(responseClass,
                                                                     url,
                                                                     body,
                                                                     ttlInMinutes
                                             );
        
        ResponseMetadata<R, E> metadata = ResponseMetadata.create(cacheToken, null);
        
        Function<E, Boolean> isNetworkError = error -> error != null && error.get(error);
        
        return observable
                .compose(errorInterceptorFactory.create(metadata))
                .compose(cacheInterceptorFactory.create(cacheToken, isNetworkError));
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
        
        @NonNull
        private final Logger logger;
        
        Factory(@NonNull ErrorInterceptor.Factory<E> errorInterceptorFactory,
                @NonNull CacheInterceptor.Factory<E> cacheInterceptorFactory,
                @NonNull Logger logger) {
            this.errorInterceptorFactory = errorInterceptorFactory;
            this.cacheInterceptorFactory = cacheInterceptorFactory;
            this.logger = logger;
        }
        
        @SuppressLint("RestrictedApi")
        public RxCacheInterceptor<E, R> create(@NonNull Class<? extends R> responseClass,
                                               @NonNull String url,
                                               @Nullable String body) {
            return new RxCacheInterceptor<>(
                    (Class<R>) responseClass,
                    url,
                    body,
                    logger,
                    errorInterceptorFactory,
                    cacheInterceptorFactory
            );
        }
    }
}
