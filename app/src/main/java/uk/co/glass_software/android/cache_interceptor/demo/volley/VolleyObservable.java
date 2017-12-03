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

import static uk.co.glass_software.android.cache_interceptor.demo.DemoPresenter.ENDPOINT;

class VolleyObservable extends Observable<JokeResponse> {
    
    private final PublishSubject<JokeResponse> publishSubject;
    private final Gson gson;
    private final RequestQueue requestQueue;
    
    VolleyObservable(Gson gson,
                     RequestQueue requestQueue) {
        this.gson = gson;
        this.requestQueue = requestQueue;
        publishSubject = PublishSubject.create();
    }
    
    @Override
    protected void subscribeActual(Observer<? super JokeResponse> observer) {
        publishSubject.subscribe(observer);
        requestQueue.add(new StringRequest(
                Request.Method.GET,
                ENDPOINT,
                this::onResponse,
                this::onError
        ));
    }
    
    private void onResponse(String response) {
        Type type = new TypeToken<JokeResponse>() {}.getType();
        JokeResponse list = gson.fromJson(response, type);
        publishSubject.onNext(list);
    }
    
    private void onError(VolleyError volleyError) {
        publishSubject.onError(volleyError);
    }
    
}
