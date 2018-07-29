package uk.co.glass_software.android.cache_interceptor.demo.volley;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.util.concurrent.Callable;

import io.reactivex.Observable;
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction;
import uk.co.glass_software.android.cache_interceptor.demo.DemoPresenter;
import uk.co.glass_software.android.cache_interceptor.demo.model.JokeResponse;
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;

import static uk.co.glass_software.android.cache_interceptor.annotations.CacheKt.DEFAULT_DURATION;

public class VolleyDemoPresenter extends DemoPresenter {

    private final static String URL = BASE_URL + ENDPOINT;
    private final RxCacheInterceptor.Factory<ApiError> rxCacheInterceptorFactory;
    private final RequestQueue requestQueue;

    public VolleyDemoPresenter(Context context,
                               Callable<String> onLogOutput) {
        super(context, onLogOutput);
        requestQueue = Volley.newRequestQueue(context);
        rxCacheInterceptorFactory = RxCacheInterceptor.Companion.buildDefault(context);
    }

    @Override
    protected Observable<JokeResponse> getResponseObservable(boolean isRefresh) {
        CacheInstruction.Operation cacheOperation = new CacheInstruction.Operation.Cache(
                false,
                DEFAULT_DURATION,
                false,
                false
        );

        CacheInstruction instruction = new CacheInstruction(
                JokeResponse.class,
                cacheOperation,
                false,
                false
        );

        return VolleyObservable.Companion.create(
                requestQueue,
                gson,
                rxCacheInterceptorFactory.create(
                        JokeResponse.class,
                        instruction,
                        URL,
                        null
                ),
                URL
        );
    }

    @Override
    public void clearEntries() {
        rxCacheInterceptorFactory.clearOlderEntries();
    }

}