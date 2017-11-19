package uk.co.glass_software.android.cache_interceptor.demo;

import android.content.Context;

import com.google.gson.Gson;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import uk.co.glass_software.android.cache_interceptor.demo.model.WeatherList;
import uk.co.glass_software.android.cache_interceptor.utils.Callback;
import uk.co.glass_software.android.cache_interceptor.utils.SimpleLogger;

public abstract class DemoPresenter {
    
    public final static String BASE_URL = "https://www.metaweather.com/";
    protected final SimpleLogger simpleLogger;
    protected final Gson gson;
    
    protected DemoPresenter(Context context,
                            Callback<String> onLogOutput) {
        simpleLogger = new SimpleLogger(context, (priority, tag, message) -> onLogOutput.call(clean(message)));
        gson = new Gson();
    }
    
    private String clean(String message) {
        return message.replaceAll("(\\([^)]+\\))", "");
    }
    
    Observable<WeatherList> loadResponse(String location) {
        return getResponseObservable(location)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    protected abstract Observable<WeatherList> getResponseObservable(String location);
}
