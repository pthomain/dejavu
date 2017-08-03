package uk.co.glass_software.android.cache_interceptor.demo;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory;
import uk.co.glass_software.android.cache_interceptor.utils.Action;
import uk.co.glass_software.android.cache_interceptor.utils.Callback;
import uk.co.glass_software.android.cache_interceptor.utils.SimpleLogger;

class MainPresenter {
    
    private final WeatherClient weatherClient;
    private final SimpleLogger simpleLogger;
    
    MainPresenter(Context context,
                  Callback<String> onHttpLog) {
        simpleLogger = new SimpleLogger(context);
        
        Retrofit retrofit = getRetrofit(context,
                                        onHttpLog,
                                        "https://www.metaweather.com/"
        );
        weatherClient = retrofit.create(WeatherClient.class);
    }
    
    private Retrofit getRetrofit(Context context,
                                 Callback<String> onHttpLog,
                                 String baseUrl) {
        RetrofitCacheAdapterFactory adapterFactory = CacheInterceptor.buildSimpleAdapterFactory(context);
        GsonConverterFactory gsonConverterFactory = GsonConverterFactory.create(new Gson());
        
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(getOkHttpClient(onHttpLog))
                .addConverterFactory(gsonConverterFactory)
                .addCallAdapterFactory(adapterFactory)
                .build();
    }
    
    @NonNull
    private OkHttpClient getOkHttpClient(Callback<String> onHttpLog) {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.followRedirects(true);
        httpClientBuilder.addInterceptor(getHttpLoggingInterceptor(onHttpLog));
        return httpClientBuilder.build();
    }
    
    @NonNull
    private HttpLoggingInterceptor getHttpLoggingInterceptor(Callback<String> onHttpLog) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(s -> {
            onHttpLog.call(s);
            simpleLogger.d(this, s);
        });
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return interceptor;
    }
    
    void loadResponse(String location,
                      Action onStart,
                      Callback<String> onNext,
                      Action onEnd) {
        weatherClient.get(location)
                     .doOnSubscribe(ignore -> onStart.act())
                     .doOnNext(response -> {
                         if (response.getMetadata().getCacheToken().getStatus().isFinal) {
                             onEnd.act();
                         }
                     })
                     .subscribeOn(Schedulers.io())
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe(response -> onNext.call(response.toString() + "\n\n" + response.getMetadata().getCacheToken().toString()));
    }
    
}
