package uk.co.glass_software.android.cache_interceptor.demo.volley;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import uk.co.glass_software.android.cache_interceptor.demo.model.JokeResponse;
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.response.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

class VolleyObservable<E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>>
        extends Observable<R> {
    
    private final PublishSubject<R> publishSubject;
    private final Gson gson;
    private final RequestQueue requestQueue;
    private final String url;
    
    private VolleyObservable(RequestQueue requestQueue,
                             Gson gson,
                             String url) {
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
        Type type = new TypeToken<JokeResponse>() {}.getType();
        R deserialisedResponse = gson.fromJson(response, type);
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
        return new VolleyObservable<E, R>(requestQueue, gson, url).compose(cacheInterceptor);
    }
}
