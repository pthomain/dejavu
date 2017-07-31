package uk.co.glass_software.android.cache_interceptor.demo;

import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.retrofit.BaseCachedResponse;

public class UserCachedResponse extends BaseCachedResponse<Exception, UserCachedResponse> {
    
    public UserCachedResponse(Exception error) {
        super(error);
    }
    
    public UserCachedResponse(Class<UserCachedResponse> responseClass,
                              String apiUrl,
                              CacheInterceptor.Factory<Exception> cacheFactory) {
        super(responseClass, apiUrl, cacheFactory);
    }
    
    @Override
    public String toString() {
        return super.toString();
    }
}
