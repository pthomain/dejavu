package uk.co.glass_software.android.cache_interceptor.interceptors.cache;

import android.content.Context;

import uk.co.glass_software.android.cache_interceptor.utils.Function;

public class CacheInterceptorBuilderHelper {
    
    public <E extends Exception & Function<E, Boolean>> CacheInterceptor.Factory<E> build(Context context,
                                                                                          CacheInterceptorBuilder.Holder holder) {
        return (CacheInterceptor.Factory<E>) new CacheInterceptorBuilder<>().build(
                context.getApplicationContext(),
                true,
                true,
                holder
        );
    }
    
}