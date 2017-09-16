package uk.co.glass_software.android.cache_interceptor.retrofit;

import java.lang.reflect.Type;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.CallAdapter;
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.response.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

class RetrofitCacheAdapter<E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>>
        implements CallAdapter<R, Object> {
    
    private final RxCacheInterceptor.Factory<E, R> rxCacheFactory;
    private final CallAdapter callAdapter;
    private final Class<R> responseClass;
    
    RetrofitCacheAdapter(RxCacheInterceptor.Factory<E, R> rxCacheFactory,
                         Class<R> responseClass,
                         CallAdapter callAdapter) {
        this.rxCacheFactory = rxCacheFactory;
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
        RxCacheInterceptor<E, R> rxCacheInterceptor = rxCacheFactory.create(responseClass,
                                                                            call.request().url().toString(),
                                                                            new String[0], //FIXME
                                                                            "" //FIXME
        );
        
        return observable.compose(rxCacheInterceptor);
    }
    
}
