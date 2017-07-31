package uk.co.glass_software.android.cache_interceptor.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.retrofit.BaseCachedResponse;
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory;
import uk.co.glass_software.android.cache_interceptor.utils.SimpleLogger;

public class MainActivity extends AppCompatActivity {
    
    private UserClient userClient;
    private View loadButton;
    private TextView resultText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        resultText = ((TextView) findViewById(R.id.result));
        
        Retrofit retrofit = getRetrofit("https://www.randomuser.me/api/?format=json");
        userClient = retrofit.create(UserClient.class);
        
        loadButton = findViewById(R.id.load_button);
        loadButton.setOnClickListener(ignore -> loadResponse());
    }
    
    private Retrofit getRetrofit(String baseUrl) {
        RetrofitCacheAdapterFactory<Exception> adapterFactory = CacheInterceptor.builder()
                                                                                .logger(new SimpleLogger(this))
                                                                                .buildAdapter(this, this::resolveApiUrl);
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(new Gson()))
                .addCallAdapterFactory(adapterFactory)
                .build();
    }
    
    private void loadResponse() {
        userClient.get()
                  .subscribeOn(Schedulers.io())
                  .doOnSubscribe(ignore -> {
                      loadButton.setEnabled(false);
                      resultText.setText("Loading...");
                  })
                  .doOnNext(ignore -> loadButton.setEnabled(true))
                  .observeOn(AndroidSchedulers.mainThread())
                  .subscribe(userCachedResponse -> resultText.setText(userCachedResponse.toString()));
    }
    
    private String resolveApiUrl(Class<? extends BaseCachedResponse> responseClass) {
        if (UserCachedResponse.class.equals(responseClass)) {
            return "/";
        }
        throw new IllegalArgumentException("Unknown response: " + responseClass);
    }
    
}
