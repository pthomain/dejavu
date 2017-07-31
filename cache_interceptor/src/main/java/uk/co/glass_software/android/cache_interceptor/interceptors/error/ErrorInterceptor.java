package uk.co.glass_software.android.cache_interceptor.interceptors.error;

import android.support.annotation.NonNull;

import java.lang.reflect.Constructor;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import uk.co.glass_software.android.cache_interceptor.retrofit.BaseCachedResponse;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

public class ErrorInterceptor<E extends Exception, R extends BaseCachedResponse<E, R>>
        implements ObservableTransformer<R, R> {
    
    private final Function<Throwable, E> errorFactory;
    private final Class<E> errorClass;
    private final Class<R> responseClass;
    private final Logger logger;
    
    private ErrorInterceptor(Function<Throwable, E> errorFactory,
                             Logger logger,
                             Class<E> errorClass,
                             Class<R> responseClass) {
        this.errorFactory = errorFactory;
        this.logger = logger;
        this.errorClass = errorClass;
        this.responseClass = responseClass;
    }
    
    @Override
    public ObservableSource<R> apply(Observable<R> upstream) {
        return upstream.onErrorResumeNext(throwable -> {
            return onError(throwable, responseClass);
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
            constructor = responseClass.getDeclaredConstructor(errorClass);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("Could not find a constructor taking a "
                                            + errorClass
                                            + " argument on "
                                            + responseClass,
                                            e
            );
        }
        
        try {
            return constructor.newInstance(apiError);
        }
        catch (Exception e) {
            throw new IllegalStateException("Could not access the error constructor on "
                                            + responseClass,
                                            e
            );
        }
    }
    
    public static class Factory<E extends Exception> {
        
        private final Function<Throwable, E> errorFactory;
        private final Logger logger;
        private final Class<E> errorClass;
        
        public Factory(Function<Throwable, E> errorFactory,
                       Logger logger,
                       Class<E> errorClass) {
            this.errorFactory = errorFactory;
            this.logger = logger;
            this.errorClass = errorClass;
        }
        
        public <R extends BaseCachedResponse<E, R>> ErrorInterceptor<E, R> create(Class<R> responseClass) {
            return new ErrorInterceptor<>(errorFactory,
                                          logger,
                                          errorClass,
                                          responseClass
            );
        }
    }
}
