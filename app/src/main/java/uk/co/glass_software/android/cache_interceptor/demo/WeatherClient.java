package uk.co.glass_software.android.cache_interceptor.demo;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;
import uk.co.glass_software.android.cache_interceptor.retrofit.SimpleCachedResponse;

public interface WeatherClient {
    
    @GET("api/location/search/")
    Observable<SimpleCachedResponse<List<Weather>>> get(@Query("query") String query);
    
}
