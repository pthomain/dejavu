package uk.co.glass_software.android.cache_interceptor.demo.retrofit;

import android.content.Context;
import android.support.annotation.NonNull;

import io.reactivex.Observable;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import uk.co.glass_software.android.cache_interceptor.demo.DemoPresenter;
import uk.co.glass_software.android.cache_interceptor.demo.model.WeatherList;
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory;
import uk.co.glass_software.android.cache_interceptor.utils.Callback;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

public class RetrofitDemoPresenter extends DemoPresenter {
    
    private final WeatherClient weatherClient;
    
    public RetrofitDemoPresenter(Context context,
                                 Callback<String> onLogOutput) {
        super(context, onLogOutput);
        weatherClient = getRetrofit(context).create(WeatherClient.class);
    }
    
    private Retrofit getRetrofit(Context context) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient(simpleLogger))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RetrofitCacheAdapterFactory.buildDefault(context))
                .build();
    }
    
    @NonNull
    private OkHttpClient getOkHttpClient(Logger logger) {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.addInterceptor(getHttpLoggingInterceptor(logger));
        httpClientBuilder.followRedirects(true);
        return httpClientBuilder.build();
    }
    
    @NonNull
    private HttpLoggingInterceptor getHttpLoggingInterceptor(Logger logger) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(s -> logger.d(this, s));
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return interceptor;
    }
    
    @Override
    protected Observable<WeatherList> getResponseObservable(String location) {
        return weatherClient.get(location);
    }
    
}
