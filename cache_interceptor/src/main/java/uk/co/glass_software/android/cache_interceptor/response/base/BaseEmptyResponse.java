package uk.co.glass_software.android.cache_interceptor.response.base;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

public class BaseEmptyResponse<E extends Exception & Function<E, Boolean>>
        implements ResponseMetadata.Holder<Void, E>  {
    
    private final BaseResponseDelegate<E, Void> delegate = new BaseResponseDelegate<>();
    
    @Override
    public float getTtlInMinutes() {
        return delegate.getTtlInMinutes();
    }
    
    @Override
    public boolean isRefresh() {
        return delegate.isRefresh();
    }
    
    @Override
    public boolean splitOnNextOnError() {
        return delegate.splitOnNextOnError();
    }
    
    @NonNull
    @Override
    public ResponseMetadata<Void, E> getMetadata() {
        return ResponseMetadata.create(CacheToken.doNotCache(Void.class), null);
    }
    
    @Override
    public void setMetadata(ResponseMetadata<Void, E> metadata) {
        delegate.setMetadata(metadata);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        BaseEmptyResponse<?> that = (BaseEmptyResponse<?>) o;
        
        return new EqualsBuilder()
                .append(delegate, that.delegate)
                .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(delegate)
                .toHashCode();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("delegate", delegate)
                .toString();
    }
}
