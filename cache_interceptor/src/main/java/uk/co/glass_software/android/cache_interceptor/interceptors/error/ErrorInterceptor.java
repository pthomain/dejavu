package uk.co.glass_software.android.cache_interceptor.interceptors.error;

import android.support.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken;
import uk.co.glass_software.android.cache_interceptor.retrofit.ResponseMetadata;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

public class ErrorInterceptor<R extends ResponseMetadata.Holder<R, E>, E extends Exception>
        implements ObservableTransformer<R, R> {
    
    private final int requestTimeout;
    private final Function<Throwable, E> errorFactory;
    private final Class<R> responseClass;
    private final Logger logger;
    
    private ErrorInterceptor(Function<Throwable, E> errorFactory,
                             Logger logger,
                             Class<R> responseClass) {
        this(errorFactory, 15, logger, responseClass);
    }
    
    private ErrorInterceptor(Function<Throwable, E> errorFactory,
                             int requestTimeout,
                             Logger logger,
                             Class<R> responseClass) {
        this.errorFactory = errorFactory;
        this.requestTimeout = requestTimeout;
        this.logger = logger;
        this.responseClass = responseClass;
    }
    
    @Override
    public ObservableSource<R> apply(Observable<R> upstream) {
        return upstream
                .timeout(requestTimeout, TimeUnit.MILLISECONDS) //fixing timeout not working in OkHttp
                .onErrorResumeNext(throwable -> {
                    return onError(throwable, responseClass);
                });
    }
    
    @NonNull
    private Observable<R> onError(Throwable throwable,
                                  Class<R> responseClass) {
        R errorResponse = getErrorResponse(throwable, responseClass);
        errorResponse.getMetadata().setCacheToken(CacheToken.doNotCache(responseClass));
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
        
        response.setMetadata(ResponseMetadata.error(responseClass, apiError));
        return response;
    }
    
    public static class Factory<E extends Exception> {
        
        private final Function<Throwable, E> errorFactory;
        private final Logger logger;
        
        public Factory(Function<Throwable, E> errorFactory,
                       Logger logger) {
            this.errorFactory = errorFactory;
            this.logger = logger;
        }
        
        public <R extends ResponseMetadata.Holder<R, E>> ErrorInterceptor<R, E> create(Class<R> responseClass) {
            return new ErrorInterceptor<>(errorFactory,
                                          logger,
                                          responseClass
            );
        }
    }
}
