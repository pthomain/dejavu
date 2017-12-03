package uk.co.glass_software.android.cache_interceptor.response;

import uk.co.glass_software.android.cache_interceptor.utils.Function;

public abstract class RefreshedResponse<E extends Exception & Function<E, Boolean>, R>
        extends CachedResponse<E, R> {
    
    @Override
    public final boolean isRefresh() {
        return true;
    }
    
}
