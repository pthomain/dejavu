package uk.co.glass_software.android.cache_interceptor.demo.retrofit;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;
import uk.co.glass_software.android.cache_interceptor.demo.model.WeatherList;

interface WeatherClient {
    
    @GET("api/location/search/")
    Observable<WeatherList> get(@Query("query") String query);
    
}
