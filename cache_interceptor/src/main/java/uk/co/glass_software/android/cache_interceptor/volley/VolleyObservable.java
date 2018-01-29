package uk.co.glass_software.android.cache_interceptor.volley;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.response.base.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

public class VolleyObservable<E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>>
        extends Observable<R> {
    
    private final PublishSubject<R> publishSubject;
    private final Gson gson;
    private final RequestQueue requestQueue;
    private final Class<R> responseClass;
    private final String url;
    
    private VolleyObservable(RequestQueue requestQueue,
                             Gson gson,
                             Class<R> responseClass,
                             String url) {
        this.responseClass = responseClass;
        this.url = url;
        this.gson = gson;
        this.requestQueue = requestQueue;
        publishSubject = PublishSubject.create();
    }
    
    @Override
    protected void subscribeActual(Observer<? super R> observer) {
        publishSubject.subscribe(observer);
        requestQueue.add(new StringRequest(
                Request.Method.GET,
                url,
                this::onResponse,
                this::onError
        ));
    }
    
    private void onResponse(String response) {
        R deserialisedResponse = gson.fromJson(response, responseClass);
        publishSubject.onNext(deserialisedResponse);
        publishSubject.onComplete();
    }
    
    private void onError(VolleyError volleyError) {
        publishSubject.onError(volleyError);
    }
    
    public static <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> Observable<R> create(RequestQueue requestQueue,
                                                                                                                             Gson gson,
                                                                                                                             RxCacheInterceptor<E, R> cacheInterceptor,
                                                                                                                             String url) {
        return new VolleyObservable<>(
                requestQueue,
                gson,
                cacheInterceptor.getResponseClass(),
                url
        ).compose(cacheInterceptor);
    }
}
