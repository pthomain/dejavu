package uk.co.glass_software.android.cache_interceptor.retrofit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

@AutoValue
public abstract class ResponseMetadata<R, E extends Exception & Function<E, Boolean>>
        implements CacheToken.Holder<R> {
    
    private CacheToken<R> cacheToken;
    
    public interface Holder<R, E extends Exception & Function<E, Boolean>> {
        @NonNull
        ResponseMetadata<R, E> getMetadata();
        
        @NonNull
        void setMetadata(ResponseMetadata<R, E> metadata);
    }
    
    ResponseMetadata() {
    }
    
    public static <R, E extends Exception & Function<E, Boolean>> ResponseMetadata<R, E> create(CacheToken<R> cacheToken,
                                                                                                E error) {
        ResponseMetadata<R, E> metadata = new AutoValue_ResponseMetadata<>(
                error,
                cacheToken.getResponseClass(),
                cacheToken.getApiUrl()
        );
        metadata.setCacheToken(cacheToken);
        return metadata;
    }
    
    @Nullable
    public abstract E getError();
    
    public abstract Class<R> getResponseClass();
    
    public abstract String getUrl();
    
    @Override
    public void setCacheToken(@NonNull CacheToken<R> cacheToken) {
        this.cacheToken = cacheToken;
    }
    
    @NonNull
    @Override
    public CacheToken<R> getCacheToken() {
        return cacheToken;
    }
    
}
