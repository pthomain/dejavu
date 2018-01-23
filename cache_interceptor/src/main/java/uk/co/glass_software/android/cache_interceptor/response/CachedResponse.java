package uk.co.glass_software.android.cache_interceptor.response;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import uk.co.glass_software.android.cache_interceptor.utils.Function;

public abstract class CachedResponse<E extends Exception & Function<E, Boolean>, R>
        implements ResponseMetadata.Holder<R, E> {
    
    private transient ResponseMetadata<R, E> metadata;
    
    @Override
    public boolean splitOnNextOnError() {
        return false;
    }
    
    @Override
    public boolean isRefresh() {
        return false;
    }
    
    @Override
    public float getTtlInMinutes() {
        return DEFAULT_TTL_IN_MINUTES;
    }
    
    @NonNull
    @Override
    public ResponseMetadata<R, E> getMetadata() {
        return metadata;
    }
    
    @Override
    public void setMetadata(@NonNull ResponseMetadata<R, E> metadata) {
        this.metadata = metadata;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        CachedResponse<?, ?> that = (CachedResponse<?, ?>) o;
        
        return new EqualsBuilder()
                .append(metadata, that.metadata)
                .isEquals();
    }
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(metadata)
                .toHashCode();
    }
    
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("metadata", metadata)
                .toString();
    }
    
}