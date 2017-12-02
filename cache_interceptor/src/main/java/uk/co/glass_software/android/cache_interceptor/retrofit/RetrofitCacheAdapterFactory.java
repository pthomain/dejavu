package uk.co.glass_software.android.cache_interceptor.retrofit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.RestrictTo;

import com.google.gson.Gson;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.reactivex.Observable;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.response.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

public class RetrofitCacheAdapterFactory<E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>>
        extends CallAdapter.Factory {
    
    private final RxJava2CallAdapterFactory rxJava2CallAdapterFactory;
    private final RxCacheInterceptor.Factory<E, R> rxCacheFactory;
    
    @RestrictTo(RestrictTo.Scope.TESTS)
    RetrofitCacheAdapterFactory(RxCacheInterceptor.Factory<E, R> rxCacheFactory) {
        this.rxCacheFactory = rxCacheFactory;
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
            
            if (ResponseMetadata.Holder.class.isAssignableFrom(rawObservableType)) {
                Class<?> responseClass = getRawType(rawObservableType);
                
                return new RetrofitCacheAdapter(
                        rxCacheFactory,
                        responseClass,
                        callAdapter
                );
            }
        }
        
        return callAdapter;
    }
    
    @SuppressLint("RestrictedApi")
    public static <R extends ResponseMetadata.Holder<R, ApiError>> RetrofitCacheAdapterFactory<ApiError, R> buildDefault(
            Context context) {
        return new RetrofitCacheAdapterFactory<>(RxCacheInterceptor.<R>buildDefault(context));
    }
    
    @SuppressLint("RestrictedApi")
    public static <R extends ResponseMetadata.Holder<R, ApiError>> RetrofitCacheAdapterFactory<ApiError, R> build(
            Context context,
            Logger logger) {
        return new RetrofitCacheAdapterFactory<>(
                RxCacheInterceptor.<ApiError, R>builder()
                        .gson(new Gson())
                        .logger(logger)
                        .errorFactory(ApiError::new)
                        .build(context)
        );
    }
}