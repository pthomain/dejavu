package uk.co.glass_software.android.cache_interceptor.demo.volley;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import io.reactivex.Observable;
import uk.co.glass_software.android.cache_interceptor.demo.DemoPresenter;
import uk.co.glass_software.android.cache_interceptor.demo.model.WeatherList;
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.utils.Callback;

public class VolleyDemoPresenter extends DemoPresenter {
    
    private final RxCacheInterceptor.Factory<ApiError, WeatherList> rxCacheInterceptorFactory;
    private final RequestQueue requestQueue;
    
    public VolleyDemoPresenter(Context context,
                               Callback<String> onLogOutput) {
        super(context, onLogOutput);
        requestQueue = Volley.newRequestQueue(context);
        rxCacheInterceptorFactory = RxCacheInterceptor.buildDefault(context);
    }
    
    @Override
    protected Observable<WeatherList> getResponseObservable(String location) {
        RxCacheInterceptor<ApiError, WeatherList> interceptor = rxCacheInterceptorFactory.create(
                WeatherList.class,
                VolleyObservable.getUrl(location),
                new String[]{location},
                ""
        );
        
        return new VolleyObservable(gson, requestQueue, location)
                .compose(interceptor);
    }
    
}