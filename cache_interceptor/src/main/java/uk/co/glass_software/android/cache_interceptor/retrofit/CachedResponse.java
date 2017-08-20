package uk.co.glass_software.android.cache_interceptor.retrofit;

import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;

public class CachedResponse<R> extends BaseCachedResponse<ApiError, R> {
    
    @Override
    public CacheToken<R> getCacheToken(String url,
                                       String[] params,
                                       String body) {
        return CacheToken.newRequest((Class<R>) CachedResponse.class,
                                     url,
                                     params
        );
    }
}
