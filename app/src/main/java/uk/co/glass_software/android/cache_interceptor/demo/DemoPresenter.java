package uk.co.glass_software.android.cache_interceptor.demo;

import android.content.Context;

import com.google.gson.Gson;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import uk.co.glass_software.android.cache_interceptor.demo.model.WeatherList;
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.utils.Action;
import uk.co.glass_software.android.cache_interceptor.utils.Callback;
import uk.co.glass_software.android.cache_interceptor.utils.SimpleLogger;

import static uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor.buildDefault;

public abstract class DemoPresenter {
    
    public final static String BASE_URL = "https://www.metaweather.com/";
    protected final SimpleLogger simpleLogger;
    protected final Gson gson;
    
    protected DemoPresenter(Context context,
                            Callback<String> onLogOutput) {
        simpleLogger = new SimpleLogger(context, (priority, tag, message) -> onLogOutput.call(message));
        gson = new Gson();
    }
    
    void loadResponse(String location,
                      Callback<String> onNext,
                      Action onEnd) {
        getResponseObservable(location)
                .doOnNext(response -> {
                    if (response.getMetadata().getCacheToken().getStatus().isFinal) {
                        onEnd.act();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> onNext.call(response.toString() + "\n\n" + response.getMetadata()
                                                                                          .getCacheToken()
                                                                                          .toString()));
    }
    
    protected abstract Observable<WeatherList> getResponseObservable(String location);
}
