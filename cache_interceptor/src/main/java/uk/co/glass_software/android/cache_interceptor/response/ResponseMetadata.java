package uk.co.glass_software.android.cache_interceptor.response;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

@AutoValue
public abstract class ResponseMetadata<R, E extends Exception & Function<E, Boolean>>
        implements CacheToken.Holder<R> {

    private CacheToken<R> cacheToken;

    public interface Holder<R, E extends Exception & Function<E, Boolean>> {

        CacheToken<R> getCacheToken(@NonNull Class<R> responseClass,
                                    @NonNull String url,
                                    @NonNull String[] params,
                                    @NonNull String body);

        @NonNull
        ResponseMetadata<R, E> getMetadata();

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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        ResponseMetadata<?, ?> that = (ResponseMetadata<?, ?>) o;
        
        return new EqualsBuilder()
                .append(cacheToken, that.cacheToken)
                .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(cacheToken)
                .toHashCode();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("cacheToken", cacheToken)
                .toString();
    }
}
