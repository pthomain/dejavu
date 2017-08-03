package uk.co.glass_software.android.cache_interceptor.demo;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;
import uk.co.glass_software.android.cache_interceptor.retrofit.CachedList;

public interface WeatherClient {
    
    @GET("api/location/search/")
    Observable<CachedList<Weather>> get(@Query("query") String query);
    
}
