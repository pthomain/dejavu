package uk.co.glass_software.android.cache_interceptor.retrofit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public abstract class CachedResponse<E extends Exception, R> implements ResponseMetadata.Holder<R, E> {
    
    @Nullable
    private R response;
    
    @NonNull
    private ResponseMetadata<R, E> metadata;
    
    @Nullable
    @Override
    public R getResponse() {
        return response;
    }
    
    @Override
    public void setResponse(@Nullable R response) {
        this.response = response;
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
        
        if (response != null ? !response.equals(that.response) : that.response != null) {
            return false;
        }
        return metadata.equals(that.metadata);
    }
    
    @Override
    public int hashCode() {
        int result = response != null ? response.hashCode() : 0;
        result = 31 * result + metadata.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CachedResponse{");
        sb.append("response=").append(response);
        sb.append(", metadata=").append(metadata);
        sb.append('}');
        return sb.toString();
    }
}
