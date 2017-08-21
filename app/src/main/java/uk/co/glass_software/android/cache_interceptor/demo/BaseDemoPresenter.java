package uk.co.glass_software.android.cache_interceptor.demo;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import uk.co.glass_software.android.cache_interceptor.demo.model.WeatherList;
import uk.co.glass_software.android.cache_interceptor.utils.Action;
import uk.co.glass_software.android.cache_interceptor.utils.Callback;

public abstract class BaseDemoPresenter implements DemoPresenter {
    
    @Override
    public void loadResponse(String location,
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
