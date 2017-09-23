package uk.co.glass_software.android.cache_interceptor.base.network.retrofit;


import io.reactivex.Observable;
import retrofit2.http.GET;
import uk.co.glass_software.android.cache_interceptor.base.network.model.TestResponse;

public interface TestClient {
    
    @GET("/")
    Observable<TestResponse> get();
    
}
