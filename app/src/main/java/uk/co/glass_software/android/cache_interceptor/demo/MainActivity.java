package uk.co.glass_software.android.cache_interceptor.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory;
import uk.co.glass_software.android.cache_interceptor.utils.SimpleLogger;

public class MainActivity extends AppCompatActivity {
    
    private SimpleLogger simpleLogger;
    private WeatherClient weatherClient;
    private View loadButton;
    private TextView resultText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        simpleLogger = new SimpleLogger(this);
        
        resultText = ((TextView) findViewById(R.id.result));
        
        Retrofit retrofit = getRetrofit("https://www.metaweather.com/");
        weatherClient = retrofit.create(WeatherClient.class);
        
        loadButton = findViewById(R.id.load_button);
        loadButton.setOnClickListener(ignore -> loadResponse());
    }
    
    private Retrofit getRetrofit(String baseUrl) {
        SimpleLogger logger = new SimpleLogger(this);
        RetrofitCacheAdapterFactory<Exception> adapterFactory = CacheInterceptor.builder()
                                                                                .logger(logger)
                                                                                .buildAdapter(this);
        
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.followRedirects(true);
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(s -> {
            appendText(s);
            logger.d(MainActivity.this, s);
        });
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        httpClientBuilder.addInterceptor(interceptor);
        
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create(new Gson()))
                .addCallAdapterFactory(adapterFactory)
                .build();
    }
    
    private void appendText(String s) {
        resultText.post(() -> resultText.setText(resultText.getText() + simpleLogger.prettyPrint(s) + "\n"));
    }
    
    private void loadResponse() {
        weatherClient.get("london")
                     .subscribeOn(Schedulers.io())
                     .doOnSubscribe(ignore -> {
                         loadButton.setEnabled(false);
                         resultText.setText("Loading...\n\n");
                     })
                     .doOnNext(ignore -> loadButton.post(() -> loadButton.setEnabled(true)))
                     .observeOn(AndroidSchedulers.mainThread())
                     .subscribe(weatherCachedResponse -> appendText(weatherCachedResponse.toString()));
    }
    
}
