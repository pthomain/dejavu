package uk.co.glass_software.android.cache_interceptor.response;

import uk.co.glass_software.android.cache_interceptor.utils.Function;

public abstract class RefreshedList<E extends Exception & Function<E, Boolean>, R, C>
        extends CachedList<E, R, C> {
    
    @Override
    public final boolean isRefresh() {
        return true;
    }
}
