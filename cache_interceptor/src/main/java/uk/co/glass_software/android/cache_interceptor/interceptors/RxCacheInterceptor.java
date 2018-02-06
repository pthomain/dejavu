package uk.co.glass_software.android.cache_interceptor.interceptors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import com.google.gson.Gson;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiErrorFactory;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor;
import uk.co.glass_software.android.cache_interceptor.response.base.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

public class RxCacheInterceptor<E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>>
        implements ObservableTransformer<R, R> {

    @NonNull
    private final Class<R> responseClass;

    @NonNull
    private final String url;

    @Nullable
    private final String body;

    private final boolean isRefresh;

    @NonNull
    private final Logger logger;

    @NonNull
    private final ErrorInterceptor.Factory<E> errorInterceptorFactory;

    @NonNull
    private final CacheInterceptor.Factory<E> cacheInterceptorFactory;

    private final boolean isCacheEnabled;

    @RestrictTo(RestrictTo.Scope.TESTS)
    RxCacheInterceptor(boolean isCacheEnabled,
                       @NonNull Class<R> responseClass,
                       @NonNull String url,
                       @Nullable String body,
                       boolean isRefresh,
                       @NonNull Logger logger,
                       @NonNull ErrorInterceptor.Factory<E> errorInterceptorFactory,
                       @NonNull CacheInterceptor.Factory<E> cacheInterceptorFactory) {
        this.isCacheEnabled = isCacheEnabled;
        this.responseClass = responseClass;
        this.url = url;
        this.body = body;
        this.isRefresh = isRefresh;
        this.logger = logger;
        this.errorInterceptorFactory = errorInterceptorFactory;
        this.cacheInterceptorFactory = cacheInterceptorFactory;
    }

    @Override
    public ObservableSource<R> apply(Observable<R> observable) {
        float ttlInMinutes;
        boolean isRefresh;
        boolean splitOnNextOnError;

        try {
            R response = responseClass.newInstance();
            ttlInMinutes = response.getTtlInMinutes();
            isRefresh = this.isRefresh || response.isRefresh();
            splitOnNextOnError = response.splitOnNextOnError();
        } catch (Exception e) {
            logger.e(this,
                    e,
                    "Could not instantiate response of type: "
                            + responseClass.getName()
                            + "; using default TTL"
            );
            ttlInMinutes = ResponseMetadata.Holder.DEFAULT_TTL_IN_MINUTES;
            isRefresh = this.isRefresh;
            splitOnNextOnError = false;
        }

        CacheToken<R> cacheToken;
        if (isCacheEnabled) {
            if (isRefresh) {
                cacheToken = CacheToken.refresh(responseClass,
                        url,
                        body,
                        ttlInMinutes
                );
            } else {
                cacheToken = CacheToken.newRequest(responseClass,
                        url,
                        body,
                        ttlInMinutes
                );
            }
        } else {
            cacheToken = CacheToken.doNotCache(responseClass);
        }

        Function<E, Boolean> isNetworkError = error -> error != null && error.get(error);

        Observable<R> interceptedObservable = observable.compose(errorInterceptorFactory.create(cacheToken))
                .compose(cacheInterceptorFactory.create(cacheToken, isNetworkError));

        if (splitOnNextOnError) {
            return interceptedObservable.flatMap(response -> response.getMetadata().hasError()
                    ? Observable.error(response.getMetadata().getError())
                    : Observable.just(response));
        }

        return interceptedObservable;
    }

    @NonNull
    public Class<R> getResponseClass() {
        return responseClass;
    }

    public static <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> RxCacheInterceptorBuilder<E, R> builder() {
        return new RxCacheInterceptorBuilder<>();
    }

    public static <R extends ResponseMetadata.Holder<R, ApiError>> RxCacheInterceptor.Factory<ApiError, R> buildDefault(Context context) {
        return RxCacheInterceptor.<ApiError, R>builder()
                .gson(new Gson())
                .errorFactory(new ApiErrorFactory())
                .build(context);
    }

    public static class Factory<E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> {

        @NonNull
        private final ErrorInterceptor.Factory<E> errorInterceptorFactory;

        @NonNull
        private final CacheInterceptor.Factory<E> cacheInterceptorFactory;

        @NonNull
        private final Logger logger;

        private final boolean isCacheEnabled;

        Factory(@NonNull ErrorInterceptor.Factory<E> errorInterceptorFactory,
                @NonNull CacheInterceptor.Factory<E> cacheInterceptorFactory,
                @NonNull Logger logger,
                boolean isCacheEnabled) {
            this.errorInterceptorFactory = errorInterceptorFactory;
            this.cacheInterceptorFactory = cacheInterceptorFactory;
            this.logger = logger;
            this.isCacheEnabled = isCacheEnabled;
        }

        @SuppressLint("RestrictedApi")
        public RxCacheInterceptor<E, R> create(@NonNull Class<R> responseClass,
                                               @NonNull String url,
                                               @Nullable String body) {
            return create(
                    responseClass,
                    url,
                    body,
                    false
            );
        }

        @SuppressLint("RestrictedApi")
        public RxCacheInterceptor<E, R> create(@NonNull Class<R> responseClass,
                                               @NonNull String url,
                                               @Nullable String body,
                                               boolean isRefresh) {
            return new RxCacheInterceptor<>(
                    isCacheEnabled,
                    responseClass,
                    url,
                    body,
                    isRefresh,
                    logger,
                    errorInterceptorFactory,
                    cacheInterceptorFactory
            );
        }

        public void clearOlderEntries() {
            cacheInterceptorFactory.clearOlderEntries();
        }

        public void flushCache() {
            cacheInterceptorFactory.flushCache();
        }
    }
}
