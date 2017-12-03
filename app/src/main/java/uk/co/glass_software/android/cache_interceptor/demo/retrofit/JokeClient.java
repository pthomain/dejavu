package uk.co.glass_software.android.cache_interceptor.demo.retrofit;

import io.reactivex.Observable;
import retrofit2.http.GET;
import uk.co.glass_software.android.cache_interceptor.demo.model.JokeResponse;
import uk.co.glass_software.android.cache_interceptor.demo.model.RefreshedJokeResponse;

import static uk.co.glass_software.android.cache_interceptor.demo.DemoPresenter.ENDPOINT;

interface JokeClient {
    
    @GET(ENDPOINT)
    Observable<JokeResponse> get();
    
    @GET(ENDPOINT)
    Observable<RefreshedJokeResponse> refresh();
    
}
