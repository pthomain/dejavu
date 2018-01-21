package uk.co.glass_software.android.cache_interceptor.interceptors.error;

import android.support.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken;
import uk.co.glass_software.android.cache_interceptor.response.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

public class ErrorInterceptor<R extends ResponseMetadata.Holder<R, E>, E extends Exception & Function<E, Boolean>>
        implements ObservableTransformer<R, R> {
    
    private final int requestTimeout;
    private final Function<Throwable, E> errorFactory;
    private final Logger logger;
    private final CacheToken<R> cacheToken;
    
    private ErrorInterceptor(Function<Throwable, E> errorFactory,
                             Logger logger,
                             CacheToken<R> cacheToken) {
        this(errorFactory, 30, logger, cacheToken);
    }
    
    private ErrorInterceptor(Function<Throwable, E> errorFactory,
                             int requestTimeout,
                             Logger logger,
                             CacheToken<R> cacheToken) {
        this.errorFactory = errorFactory;
        this.requestTimeout = requestTimeout;
        this.logger = logger;
        this.cacheToken = cacheToken;
    }
    
    @Override
    public ObservableSource<R> apply(Observable<R> upstream) {
        return upstream
                .filter(o -> o != null) //see https://github.com/square/retrofit/issues/2242
                .switchIfEmpty(Observable.error(new NoSuchElementException("Response was empty")))
                .timeout(requestTimeout, TimeUnit.SECONDS) //fixing timeout not working in OkHttp
                .doOnNext(response -> response.setMetadata(ResponseMetadata.create(cacheToken, null)))
                .onErrorResumeNext(throwable -> {
                    return onError(throwable, cacheToken.getResponseClass());
                });
    }
    
    @NonNull
    private Observable<R> onError(Throwable throwable,
                                  Class<R> responseClass) {
        R errorResponse = getErrorResponse(throwable, responseClass);
        return Observable.just(errorResponse);
    }
    
    @NonNull
    private R getErrorResponse(Throwable throwable,
                               Class<R> responseClass) {
        E apiError = errorFactory.get(throwable);
        Constructor<R> constructor;
        
        logger.e(this,
                 throwable,
                 "An error occurred during the network request for " + responseClass
        );
        
        
        try {
            constructor = responseClass.getDeclaredConstructor();
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("Could not find a public constructor for "
                                            + responseClass,
                                            e
            );
        }
        R response;
        try {
            response = constructor.newInstance();
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not access the error constructor on "
                                            + responseClass,
                                            e
            );
        }
        
        CacheToken<R> cacheToken = CacheToken.doNotCache(responseClass);
        response.setMetadata(ResponseMetadata.create(cacheToken, apiError));
        return response;
    }
    
    public static class Factory<E extends Exception & Function<E, Boolean>> {
        
        private final Function<Throwable, E> errorFactory;
        private final Logger logger;
        
        public Factory(Function<Throwable, E> errorFactory,
                       Logger logger) {
            this.errorFactory = errorFactory;
            this.logger = logger;
        }
        
        public <R extends ResponseMetadata.Holder<R, E>> ErrorInterceptor<R, E> create(CacheToken<R> cacheToken) {
            return new ErrorInterceptor<>(
                    errorFactory,
                    logger,
                    cacheToken
            );
        }
    }
}
