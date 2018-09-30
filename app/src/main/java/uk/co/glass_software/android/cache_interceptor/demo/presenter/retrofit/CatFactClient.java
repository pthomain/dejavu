package uk.co.glass_software.android.cache_interceptor.demo.presenter.retrofit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import uk.co.glass_software.android.cache_interceptor.annotations.Cache;
import uk.co.glass_software.android.cache_interceptor.annotations.Clear;
import uk.co.glass_software.android.cache_interceptor.annotations.Refresh;
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse;

import static uk.co.glass_software.android.cache_interceptor.demo.presenter.BaseDemoPresenter.ENDPOINT;

interface CatFactClient {

    @GET(ENDPOINT)
    @Cache()
    Observable<CatFactResponse> get();

    @GET(ENDPOINT)
    @Refresh
    Observable<CatFactResponse> refresh();

    @GET(ENDPOINT)
    @Cache(freshOnly = true)
    Observable<CatFactResponse> getFreshOnly();

    @GET(ENDPOINT)
    @Refresh(freshOnly = true)
    Observable<CatFactResponse> refreshFreshOnly();

    @DELETE(ENDPOINT)
    @Clear(typeToClear = CatFactResponse.class)
    Completable clearCache();

}
