package uk.co.glass_software.android.cache_interceptor.retrofit;

import java.lang.reflect.Type;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.CallAdapter;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor;
import uk.co.glass_software.android.utils.Function;

class RetrofitCacheAdapter<E extends Exception, R extends BaseCachedResponse<E, R>>
        implements CallAdapter<R, Object> {
    
    private final Class<R> responseClass;
    private final Function<Class<R>, String> apiUrlResolver;
    private final Function<E, Boolean> isNetworkError;
    private final CallAdapter callAdapter;
    private final ErrorInterceptor.Factory<E> errorFactory;
    private final CacheInterceptor.Factory interceptorFactory;
    
    RetrofitCacheAdapter(ErrorInterceptor.Factory<E> errorFactory,
                         CacheInterceptor.Factory interceptorFactory,
                         Class<R> responseClass,
                         Function<Class<R>, String> apiUrlResolver,
                         Function<E, Boolean> isNetworkError,
                         CallAdapter callAdapter) {
        this.errorFactory = errorFactory;
        this.interceptorFactory = interceptorFactory;
        this.responseClass = responseClass;
        this.apiUrlResolver = apiUrlResolver;
        this.isNetworkError = isNetworkError;
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
        return observable
                .compose(errorFactory.create(responseClass))
                .compose(interceptorFactory.create(
                        responseClass,
                        apiUrlResolver.get(responseClass),
                        isNetworkError
                ));
    }
}
