package uk.co.glass_software.android.cache_interceptor.demo.model;

import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.response.CachedList;

public class WeatherList extends CachedList<ApiError, WeatherList, Weather> {
    
    @Override
    public float getTtlInMinutes() {
        return 1f / 6f; //10s
    }
    
}