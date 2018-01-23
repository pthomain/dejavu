package uk.co.glass_software.android.cache_interceptor.response;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;

import uk.co.glass_software.android.cache_interceptor.utils.Function;

public abstract class CachedList<E extends Exception & Function<E, Boolean>, R, C>
        extends ArrayList<C>
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
    public String toString() {
        return new ToStringBuilder(this)
                .append("metadata", metadata)
                .toString();
    }
}
