package uk.co.glass_software.android.cache_interceptor.demo.volley;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import io.reactivex.Observable;
import uk.co.glass_software.android.cache_interceptor.demo.DemoPresenter;
import uk.co.glass_software.android.cache_interceptor.demo.model.JokeResponse;
import uk.co.glass_software.android.cache_interceptor.demo.model.RefreshedJokeResponse;
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.utils.Callback;

public class VolleyDemoPresenter extends DemoPresenter {
    
    private final RxCacheInterceptor.Factory<ApiError, JokeResponse> rxCacheInterceptorFactory;
    private final RequestQueue requestQueue;
    
    public VolleyDemoPresenter(Context context,
                               Callback<String> onLogOutput) {
        super(context, onLogOutput);
        requestQueue = Volley.newRequestQueue(context);
        rxCacheInterceptorFactory = RxCacheInterceptor.<ApiError, JokeResponse>builder()
                .logger(simpleLogger)
                .errorFactory(ApiError::new)
                .build(context);
    }
    
    @Override
    protected Observable<? extends JokeResponse> getResponseObservable(boolean isRefresh) {
        RxCacheInterceptor<ApiError, JokeResponse> interceptor = rxCacheInterceptorFactory.create(
                isRefresh ? RefreshedJokeResponse.class : JokeResponse.class,
                BASE_URL + ENDPOINT,
                null
        );
        
        return new VolleyObservable(gson, requestQueue).compose(interceptor);
    }
    
}