package uk.co.glass_software.android.cache_interceptor.demo;

import android.content.Context;

import com.google.gson.Gson;

import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import uk.co.glass_software.android.cache_interceptor.demo.model.JokeResponse;
import uk.co.glass_software.android.cache_interceptor.utils.Callback;
import uk.co.glass_software.android.shared_preferences.utils.SimpleLogger;

public abstract class DemoPresenter {

    protected final static String BASE_URL = "http://api.icndb.com/";
    public final static String ENDPOINT = "jokes/random";

    protected final SimpleLogger simpleLogger;
    protected final Gson gson;

    protected DemoPresenter(Context context,
                            Callable<String> onLogOutput) {
        simpleLogger = new SimpleLogger(context, (priority, tag, message) -> onLogOutput.call(clean(message)));
        gson = new Gson();
    }

    private String clean(String message) {
        return message.replaceAll("(\\([^)]+\\))", "");
    }

    Observable<? extends JokeResponse> loadResponse(boolean isRefresh) {
        return getResponseObservable(isRefresh)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    protected abstract Observable<? extends JokeResponse> getResponseObservable(boolean isRefresh);

    public abstract void clearEntries();

}
