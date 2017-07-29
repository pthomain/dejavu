package uk.co.glass_software.android.cache_interceptor.retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.reactivex.Observable;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor;
import uk.co.glass_software.android.utils.Function;

public class RetrofitCacheAdapterFactory<E extends Exception> extends CallAdapter.Factory {
    
    private final RxJava2CallAdapterFactory rxJava2CallAdapterFactory;
    private final ErrorInterceptor.Factory<E> errorFactory;
    private final CacheInterceptor.Factory cacheInterceptorFactory;
    private final Function<Class<? extends BaseCachedResponse>, String> apiUrlResolver;
    private final Function<?, Boolean> isNetworkError;
    
    public RetrofitCacheAdapterFactory(ErrorInterceptor.Factory<E> errorFactory,
                                       CacheInterceptor.Factory<E> cacheInterceptorFactory,
                                       Function<Class<? extends BaseCachedResponse>, String> apiUrlResolver,
                                       Function<E, Boolean> isNetworkError) {
        this.errorFactory = errorFactory;
        this.cacheInterceptorFactory = cacheInterceptorFactory;
        this.apiUrlResolver = apiUrlResolver;
        this.isNetworkError = isNetworkError;
        rxJava2CallAdapterFactory = RxJava2CallAdapterFactory.create();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public CallAdapter<?, ?> get(Type returnType,
                                 Annotation[] annotations,
                                 Retrofit retrofit) {
        Class<?> rawType = getRawType(returnType);
        CallAdapter callAdapter = rxJava2CallAdapterFactory.get(returnType, annotations, retrofit);
        
        if (rawType == Observable.class
            && returnType instanceof ParameterizedType) {
            
            Type observableType = getParameterUpperBound(0, (ParameterizedType) returnType);
            Class<?> rawObservableType = getRawType(observableType);
            
            if (rawObservableType == BaseCachedResponse.class) {
                if (!(observableType instanceof ParameterizedType)) {
                    throw new IllegalStateException("Response must be parameterized as BaseCachedResponse<Foo>");
                }
                Type responseType = getParameterUpperBound(0, (ParameterizedType) observableType);
                
                Class<? extends BaseCachedResponse> responseClass = (Class<? extends BaseCachedResponse>) getRawType(responseType);
                
                return new RetrofitCacheAdapter(errorFactory,
                                                cacheInterceptorFactory,
                                                responseClass,
                                                apiUrlResolver,
                                                isNetworkError,
                                                callAdapter
                );
            }
        }
        
        return callAdapter;
    }
    
}