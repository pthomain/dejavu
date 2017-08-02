package uk.co.glass_software.android.cache_interceptor.retrofit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.io.IOException;

import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

@AutoValue
public abstract class ResponseMetadata<R, E extends Exception>
        implements CacheToken.Holder<R> {
    
    private CacheToken<R> cacheToken;
    
    public interface Holder<R, E extends Exception> {
        @Nullable
        R getResponse();
        
        void setResponse(@Nullable R response);
        
        @NonNull
        ResponseMetadata<R, E> getMetadata();
        
        @NonNull
        void setMetadata(ResponseMetadata<R, E> metadata);
    }
    
    ResponseMetadata() {
    }
    
    public static <R, E extends Exception> ResponseMetadata<R, E> error(Class<R> responseClass,
                                                                        E error) {
        return create(CacheToken.doNotCache(responseClass),
                      e -> e instanceof IOException,
                      error
        );
    }
    
    public static <R, E extends Exception> ResponseMetadata<R, E> create(CacheToken<R> cacheToken,
                                                                         Function<E, Boolean> isNetworkError,
                                                                         E error) {
        ResponseMetadata<R, E> metadata = new AutoValue_ResponseMetadata<>(
                isNetworkError,
                error,
                cacheToken.getResponseClass(),
                cacheToken.getApiUrl()
        );
        metadata.setCacheToken(cacheToken);
        return metadata;
    }
    
    public abstract Function<E, Boolean> isNetworkError();
    
    @Nullable
    public abstract E getError();
    
    public abstract Class<R> getResponseClass();
    
    public abstract String getUrl();
    
    @Override
    public void setCacheToken(@NonNull CacheToken<R> cacheToken) {
        this.cacheToken = cacheToken;
    }
    
    @Override
    public CacheToken<R> getCacheToken() {
        return cacheToken;
    }
    
}
