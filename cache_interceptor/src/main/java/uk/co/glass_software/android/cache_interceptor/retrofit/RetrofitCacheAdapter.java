package uk.co.glass_software.android.cache_interceptor.retrofit;

import java.lang.reflect.Type;

import io.reactivex.Observable;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.CallAdapter;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

class RetrofitCacheAdapter<E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>>
        implements CallAdapter<R, Object> {
    
    private final Class<R> responseClass;
    private final CallAdapter callAdapter;
    private final ErrorInterceptor.Factory<E> errorFactory;
    private final CacheInterceptor.Factory cacheFactory;
    
    RetrofitCacheAdapter(ErrorInterceptor.Factory<E> errorFactory,
                         CacheInterceptor.Factory cacheFactory,
                         Class<R> responseClass,
                         CallAdapter callAdapter) {
        this.errorFactory = errorFactory;
        this.cacheFactory = cacheFactory;
        this.responseClass = responseClass;
        this.callAdapter = callAdapter;
    }
    
    @Override
    public Type responseType() {
        return callAdapter.responseType();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Object adapt(Call<R> call) {
        Observable<R> observable = (Observable<R>) callAdapter.adapt(call);
        Request request = call.request();
        
        CacheToken<R> cacheToken = CacheToken.newRequest(responseClass,
                                                         request.url().toString()
        );
        
        ResponseMetadata<R, E> metadata = ResponseMetadata.create(cacheToken,
                                                                  null
        );
        
        return observable
                .compose(errorFactory.create(metadata))
                .compose(cacheFactory.create(cacheToken));
    }
}
