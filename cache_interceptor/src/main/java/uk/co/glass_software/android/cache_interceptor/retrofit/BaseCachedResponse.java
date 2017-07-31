package uk.co.glass_software.android.cache_interceptor.retrofit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import io.reactivex.Observable;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken;

public abstract class BaseCachedResponse<E extends Exception, R extends BaseCachedResponse<E, R>>
        implements CacheToken.Holder<R, E> {
    
    @Nullable
    private final CacheInterceptor<E, R> interceptor;
    
    @Nullable
    private final E error;
    
    private CacheToken<R> cacheToken;
    
    protected BaseCachedResponse(@NonNull E error) {
        this.error = error;
        interceptor = null;
    }
    
    protected BaseCachedResponse(Class<R> responseClass,
                                 String apiUrl,
                                 CacheInterceptor.Factory<E> cacheFactory) {
        error = null;
        interceptor = cacheFactory.create(responseClass,
                                          apiUrl,
                                          this::isNetworkError
        );
    }
    
    private Boolean isNetworkError(E exception) {
        return exception instanceof IOException;
    }
    
    public Observable<R> intercept(Observable<R> originalObservable) {
        if (interceptor != null) {
            return originalObservable.compose(interceptor);
        }
        return originalObservable;
    }
    
    @Nullable
    public CacheInterceptor<E, R> getInterceptor() {
        return interceptor;
    }
    
    @Override
    public void setCacheToken(@NonNull CacheToken<R> cacheToken) {
        this.cacheToken = cacheToken;
    }
    
    @Override
    public CacheToken<R> getCacheToken() {
        return cacheToken;
    }
    
    @Nullable
    @Override
    public E getError() {
        return error;
    }
    
    @Override
    public String toString() {
        return "BaseCachedResponse{" +
               "error=" + error +
               ", token=" + cacheToken +
               '}';
    }
}
