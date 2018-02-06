package uk.co.glass_software.android.cache_interceptor.retrofit;

import android.annotation.SuppressLint;
import android.content.Context;

import com.google.gson.Gson;

import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import uk.co.glass_software.android.cache_interceptor.R;
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.response.base.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;
import uk.co.glass_software.android.cache_interceptor.utils.SimpleLogger;

public class RetrofitCacheAdapterFactoryBuilder<E extends Exception & Function<E, Boolean>> {

    private final Context context;
    private Function<Throwable, E> errorFactory;
    private Gson gson;
    private Logger logger;

    RetrofitCacheAdapterFactoryBuilder(Context context,
                                       Function<Throwable, E> errorFactory) {
        this.context = context;
        this.errorFactory = errorFactory;
    }

    public RetrofitCacheAdapterFactoryBuilder<E> gson(Gson gson) {
        this.gson = gson;
        return this;
    }

    public RetrofitCacheAdapterFactoryBuilder<E> logger(Logger logger) {
        this.logger = logger;
        return this;
    }

    @SuppressLint("RestrictedApi")
    public <R extends ResponseMetadata.Holder<R, E>> RetrofitCacheAdapterFactory<E, R> build() {
        RxCacheInterceptor.Factory<E, R> cacheInterceptorFactory = RxCacheInterceptor.<E, R>builder()
                .gson(gson == null ? new Gson() : gson)
                .logger(logger == null ? new SimpleLogger(context) : logger)
                .errorFactory(errorFactory)
                .build(context);

        return new RetrofitCacheAdapterFactory<>(
                RxJava2CallAdapterFactory.create(),
                cacheInterceptorFactory
        );
    }
}
