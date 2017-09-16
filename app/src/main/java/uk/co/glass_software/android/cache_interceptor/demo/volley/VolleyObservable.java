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
import uk.co.glass_software.android.cache_interceptor.demo.model.WeatherList;

import static uk.co.glass_software.android.cache_interceptor.demo.DemoPresenter.BASE_URL;

class VolleyObservable extends Observable<WeatherList> {
    
    private final PublishSubject<WeatherList> publishSubject;
    private final Gson gson;
    private final RequestQueue requestQueue;
    private final String location;
    
    VolleyObservable(Gson gson,
                     RequestQueue requestQueue,
                     String location) {
        this.gson = gson;
        this.requestQueue = requestQueue;
        this.location = location;
        publishSubject = PublishSubject.create();
    }
    
    @Override
    protected void subscribeActual(Observer<? super WeatherList> observer) {
        publishSubject.subscribe(observer);
        requestQueue.add(new StringRequest(Request.Method.GET,
                                           getUrl(location),
                                           this::onResponse,
                                           this::onError
        ));
    }
    
    static String getUrl(String location) {
        return String.format(BASE_URL + "api/location/search/?query=%s", location);
    }
    
    private void onResponse(String response) {
        Type type = new TypeToken<WeatherList>() {
        }.getType();
        WeatherList list = gson.fromJson(response, type);
        publishSubject.onNext(list);
    }
    
    private void onError(VolleyError volleyError) {
        publishSubject.onError(volleyError);
    }
    
}
