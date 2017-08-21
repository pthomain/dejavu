package uk.co.glass_software.android.cache_interceptor.demo.volley;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.common.reflect.TypeToken;

import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import uk.co.glass_software.android.cache_interceptor.demo.ServiceLocator;
import uk.co.glass_software.android.cache_interceptor.demo.model.WeatherList;
import uk.co.glass_software.android.cache_interceptor.utils.Callback;

//TODO move to lib
class VolleyObservable extends Observable<WeatherList> {
    
    private final PublishSubject<WeatherList> publishSubject;
    private final ServiceLocator serviceLocator;
    private final RequestQueue requestQueue;
    private final String location;
    
    VolleyObservable(ServiceLocator serviceLocator,
                     Context context,
                     Callback<String> onLogOutput,
                     String location) {
        this.serviceLocator = serviceLocator;
        this.location = location;
        requestQueue = serviceLocator.getRequestQueue(context);
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
    
    private void onResponse(String response) {
        Type type = new TypeToken<WeatherList>() {}.getType();
        WeatherList list = serviceLocator.getGson().fromJson(response, type);
        publishSubject.onNext(list);
    }
    
    private void onError(VolleyError volleyError) {
        publishSubject.onError(volleyError);
    }
    
    @NonNull
    public static String getUrl(String param) {
        return String.format(ServiceLocator.baseUrl + "api/location/search/?query=%s", param);
    }
}
