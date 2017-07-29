package uk.co.glass_software.android.cache_interceptor.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.gson.Gson;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.retrofit.BaseCachedResponse;
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory;
import uk.co.glass_software.android.utils.SimpleLogger;

public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Retrofit retrofit = getRetrofit("https://randomuser.me/api/?format=json");
        UserClient userClient = retrofit.create(UserClient.class);
        userClient.get().subscribe(this::onUserResponse);
    }
    
    private Retrofit getRetrofit(String baseUrl) {
        RetrofitCacheAdapterFactory<Exception> adapterFactory = getCacheAdapter();
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(new Gson()))
                .addCallAdapterFactory(adapterFactory)
                .build();
    }
    
    private RetrofitCacheAdapterFactory<Exception> getCacheAdapter() {
        return CacheInterceptor.builder()
                               .logger(new SimpleLogger(this))
                               .buildAdapter(this, this::resolveApiUrl);
    }
    
    
    private void onUserResponse(UserCachedResponse userCachedResponse) {
        Toast.makeText(this,
                       userCachedResponse.toString(),
                       Toast.LENGTH_LONG
        ).show();
    }
    
    private String resolveApiUrl(Class<? extends BaseCachedResponse> responseClass) {
        if (UserCachedResponse.class.equals(responseClass)) {
            return "/";
        }
        throw new IllegalArgumentException("Unknown response: " + responseClass);
    }
    
}
