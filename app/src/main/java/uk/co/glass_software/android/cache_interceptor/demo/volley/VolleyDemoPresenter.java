package uk.co.glass_software.android.cache_interceptor.demo.volley;

import android.content.Context;

import io.reactivex.Observable;
import uk.co.glass_software.android.cache_interceptor.demo.BaseDemoPresenter;
import uk.co.glass_software.android.cache_interceptor.demo.ServiceLocator;
import uk.co.glass_software.android.cache_interceptor.demo.model.WeatherList;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor;
import uk.co.glass_software.android.cache_interceptor.response.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Callback;
import uk.co.glass_software.android.cache_interceptor.utils.SimpleLogger;

import static uk.co.glass_software.android.cache_interceptor.demo.volley.VolleyObservable.getUrl;

public class VolleyDemoPresenter extends BaseDemoPresenter {
    
    private final Context context;
    private final Callback<String> onLogOutput;
    private final ServiceLocator serviceLocator;
    private final ErrorInterceptor.Factory<ApiError> errorFactoryFactory;
    private final CacheInterceptor.Factory<ApiError> cacheFactory;
    
    public VolleyDemoPresenter(Context context,
                               Callback<String> onLogOutput) {
        serviceLocator = new ServiceLocator(context);
        this.context = context;
        this.onLogOutput = onLogOutput;
        SimpleLogger logger = serviceLocator.getLogger(context, onLogOutput);
        errorFactoryFactory = new ErrorInterceptor.Factory<>(ApiError::new, logger);
        cacheFactory = serviceLocator.getCacheInterceptorBuilder(context, onLogOutput)
                                     .build(context);
    }
    
    @Override
    protected Observable<WeatherList> getResponseObservable(String location) {
        CacheToken<WeatherList> cacheToken = getCacheToken(location);
        ResponseMetadata<WeatherList, ApiError> metadata = ResponseMetadata.create(cacheToken,
                                                                                   null
        );
        return new VolleyObservable(serviceLocator,
                                    context,
                                    onLogOutput,
                                    location
        ).compose(errorFactoryFactory.create(metadata))
         .compose(cacheFactory.create(cacheToken));
    }
    
    private CacheToken<WeatherList> getCacheToken(String location) {
        return new WeatherList().getCacheToken(WeatherList.class,
                                               getUrl(location),
                                               new String[0],
                                               ""
        );
    }
    
}