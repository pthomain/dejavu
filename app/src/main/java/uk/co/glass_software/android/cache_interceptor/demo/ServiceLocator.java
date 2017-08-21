package uk.co.glass_software.android.cache_interceptor.demo;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import uk.co.glass_software.android.cache_interceptor.demo.retrofit.WeatherClient;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptorBuilder;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory;
import uk.co.glass_software.android.cache_interceptor.utils.Callback;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;
import uk.co.glass_software.android.cache_interceptor.utils.SimpleLogger;

public class ServiceLocator {
    
    public final static String baseUrl = "https://www.metaweather.com/";
    private final SimpleLogger internalLogger;
    private final Gson gson;
    
    public ServiceLocator(Context context) {
        internalLogger = new SimpleLogger(context);
        gson = new Gson();
    }
    
    public RequestQueue getRequestQueue(Context context) {
        return Volley.newRequestQueue(context);
    }
    
    String prettyPrint(String output) {
        return internalLogger.prettyPrint(output);
    }
    
    public WeatherClient getWeatherClient(Retrofit retrofit) {
        return retrofit.create(WeatherClient.class);
    }
    
    public CacheInterceptorBuilder<ApiError> getCacheInterceptorBuilder(Context context,
                                                                        Callback<String> onLogOutput) {
        SimpleLogger logger = getLogger(context, onLogOutput);
        return CacheInterceptor.builder().logger(logger);
    }
    
    public Retrofit getRetrofit(Context context,
                                Callback<String> onLogOutput) {
        SimpleLogger logger = getLogger(context, onLogOutput);
        RetrofitCacheAdapterFactory cacheFactory = getCacheInterceptorBuilder(context,
                                                                              onLogOutput
        ).buildAdapter(context);
        
        GsonConverterFactory gsonConverterFactory = GsonConverterFactory.create(gson);
        
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(getOkHttpClient(logger))
                .addConverterFactory(gsonConverterFactory)
                .addCallAdapterFactory(cacheFactory)
                .build();
    }
    
    @NonNull
    private OkHttpClient getOkHttpClient(Logger logger) {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.followRedirects(true);
        httpClientBuilder.addInterceptor(getHttpLoggingInterceptor(logger));
        return httpClientBuilder.build();
    }
    
    @NonNull
    private HttpLoggingInterceptor getHttpLoggingInterceptor(Logger logger) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(s -> logger.d(this, s));
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return interceptor;
    }
    
    public SimpleLogger getLogger(Context context,
                                  Callback<String> onLogOutput) {
        return new SimpleLogger(context,
                                (priority, tag, message) -> onLogOutput.call(message)
        );
    }
    
    public Gson getGson() {
        return gson;
    }
    
}
