package uk.co.glass_software.android.cache_interceptor.response.base;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import uk.co.glass_software.android.cache_interceptor.utils.Function;

public abstract class BaseCachedResponse<E extends Exception & Function<E, Boolean>, R>
        implements ResponseMetadata.Holder<R, E> {
    
    private final BaseResponseDelegate<E, R> delegate = new BaseResponseDelegate<>();
    
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
    public ResponseMetadata<R, E> getMetadata() {
        return delegate.getMetadata();
    }
    
    @Override
    public void setMetadata(ResponseMetadata<R, E> metadata) {
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
        
        BaseCachedResponse<?, ?> that = (BaseCachedResponse<?, ?>) o;
        
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