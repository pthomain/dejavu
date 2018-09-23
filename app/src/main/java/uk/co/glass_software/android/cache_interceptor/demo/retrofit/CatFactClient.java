package uk.co.glass_software.android.cache_interceptor.demo.retrofit;

import io.reactivex.Observable;
import retrofit2.http.GET;
import uk.co.glass_software.android.cache_interceptor.annotations.Cache;
import uk.co.glass_software.android.cache_interceptor.annotations.Clear;
import uk.co.glass_software.android.cache_interceptor.annotations.Refresh;
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse;

import static uk.co.glass_software.android.cache_interceptor.demo.DemoPresenter.ENDPOINT;

interface CatFactClient {

    @Cache(encrypt = true, mergeOnNextOnError = true)
    @GET(ENDPOINT)
    Observable<CatFactResponse> get();

    @Refresh(freshOnly = true)
    @GET(ENDPOINT)
    Observable<CatFactResponse> refresh();

    @Clear
    void clearCache();

}
