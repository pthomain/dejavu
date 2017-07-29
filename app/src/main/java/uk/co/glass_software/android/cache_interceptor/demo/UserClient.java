package uk.co.glass_software.android.cache_interceptor.demo;

import io.reactivex.Observable;
import retrofit2.http.GET;

public interface UserClient {
    
    @GET
    Observable<UserCachedResponse> get();
    
}
