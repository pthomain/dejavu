package uk.co.glass_software.android.cache_interceptor.demo.model;

import android.support.annotation.NonNull;

import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.response.CachedList;

public class WeatherList extends CachedList<ApiError, WeatherList, Weather> {
    
    @Override
    public CacheToken<WeatherList> getCacheToken(@NonNull Class<WeatherList> responseClass,
                                                 @NonNull String url,
                                                 @NonNull String[] params,
                                                 @NonNull String body) {
        return CacheToken.newRequest(responseClass,
                                     url,
                                     params
        );
    }
}
