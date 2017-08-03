package uk.co.glass_software.android.cache_interceptor.retrofit;

import android.support.annotation.NonNull;

import java.util.ArrayList;

import uk.co.glass_software.android.cache_interceptor.utils.Function;

public abstract class BaseCachedList<E extends Exception & Function<E, Boolean>, R> extends ArrayList<R>
        implements ResponseMetadata.Holder<R, E> {
    
    private ResponseMetadata<R, E> metadata;
    
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
        if (!super.equals(o)) {
            return false;
        }
        
        BaseCachedList<?, ?> that = (BaseCachedList<?, ?>) o;
        
        return metadata != null ? metadata.equals(that.metadata) : that.metadata == null;
    }
    
    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
        return "BaseCachedList{\nmetadata=" + metadata + "\n}";
    }
}
