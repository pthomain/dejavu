package uk.co.glass_software.android.cache_interceptor.retrofit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.RestrictTo;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import io.reactivex.Observable;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import uk.co.glass_software.android.cache_interceptor.R;
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.response.base.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

public class RetrofitCacheAdapterFactory<E extends Exception & Function<E, Boolean>>
        extends CallAdapter.Factory {

    private final RxJava2CallAdapterFactory rxJava2CallAdapterFactory;
    private final RxCacheInterceptor.Factory<E> rxCacheFactory;

    @RestrictTo(RestrictTo.Scope.TESTS)
    RetrofitCacheAdapterFactory(RxJava2CallAdapterFactory rxJava2CallAdapterFactory,
                                RxCacheInterceptor.Factory<E> rxCacheFactory) {
        this.rxJava2CallAdapterFactory = rxJava2CallAdapterFactory;
        this.rxCacheFactory = rxCacheFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    @SuppressLint("RestrictedApi")
    public CallAdapter<?, ?> get(Type returnType,
                                 Annotation[] annotations,
                                 Retrofit retrofit) {
        Class<?> rawType = getRawType(returnType);
        CallAdapter callAdapter = getCallAdapter(returnType, annotations, retrofit);

        if (rawType == Observable.class
                && returnType instanceof ParameterizedType) {

            Type observableType = getParameterUpperBound(0, (ParameterizedType) returnType);
            Class<?> rawObservableType = getRawType(observableType);

            if (ResponseMetadata.Holder.class.isAssignableFrom(rawObservableType)) {
                Class responseClass = getRawType(rawObservableType);

                return create(
                        rxCacheFactory,
                        responseClass,
                        callAdapter
                );
            }
        }

        return callAdapter;
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    CallAdapter<?, ?> getCallAdapter(Type returnType,
                                     Annotation[] annotations,
                                     Retrofit retrofit) {
        return rxJava2CallAdapterFactory.get(returnType, annotations, retrofit);
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    <R extends ResponseMetadata.Holder<R, E>> RetrofitCacheAdapter<E, R> create(RxCacheInterceptor.Factory<E> rxCacheFactory,
                                                                                Class<R> responseClass,
                                                                                CallAdapter callAdapter) {
        return new RetrofitCacheAdapter<>(
                rxCacheFactory,
                responseClass,
                callAdapter
        );
    }

    @SuppressLint("RestrictedApi")
    public static <R extends ResponseMetadata.Holder<R, ApiError>> RetrofitCacheAdapterFactory<ApiError> buildDefault(Context context) {
        return new RetrofitCacheAdapterFactory<>(
                RxJava2CallAdapterFactory.create(),
                RxCacheInterceptor.<R>buildDefault(context)
        );
    }

    @SuppressLint("RestrictedApi")
    public static <E extends Exception & Function<E, Boolean>> RetrofitCacheAdapterFactoryBuilder<E> build(Context context,
                                                                                                           Function<Throwable, E> errorFactory) {
        return new RetrofitCacheAdapterFactoryBuilder<>(context, errorFactory);
    }

    public void clearOlderEntries() {
        rxCacheFactory.clearOlderEntries();
    }

    public void flushCache() {
        rxCacheFactory.flushCache();
    }
}