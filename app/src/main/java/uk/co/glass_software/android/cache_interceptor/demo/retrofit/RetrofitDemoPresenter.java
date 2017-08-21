package uk.co.glass_software.android.cache_interceptor.demo.retrofit;

import android.content.Context;

import io.reactivex.Observable;
import retrofit2.Retrofit;
import uk.co.glass_software.android.cache_interceptor.demo.BaseDemoPresenter;
import uk.co.glass_software.android.cache_interceptor.demo.ServiceLocator;
import uk.co.glass_software.android.cache_interceptor.demo.model.WeatherList;
import uk.co.glass_software.android.cache_interceptor.utils.Callback;

public class RetrofitDemoPresenter extends BaseDemoPresenter {
    
    private final WeatherClient weatherClient;
    
    public RetrofitDemoPresenter(Context context,
                                 Callback<String> onLogOutput) {
        ServiceLocator serviceLocator = new ServiceLocator(context);
        Retrofit retrofit = serviceLocator.getRetrofit(context, onLogOutput);
        weatherClient = serviceLocator.getWeatherClient(retrofit);
    }
    
    @Override
    protected Observable<WeatherList> getResponseObservable(String location) {
        return weatherClient.get(location);
    }
    
}
