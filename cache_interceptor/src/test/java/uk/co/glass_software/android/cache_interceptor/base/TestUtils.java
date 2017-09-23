package uk.co.glass_software.android.cache_interceptor.base;


import org.mockito.ArgumentCaptor;

import io.reactivex.Observable;
import io.reactivex.Observer;
import uk.co.glass_software.android.cache_interceptor.utils.Action;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class TestUtils {
    
    /**
     * Convenience method used to expect exceptions on calls. This is for RxJava calls since the
     * exception mechanism is delivered onError. For calls to Observable.blockingFirst(), the exception
     * is thrown as a RuntimeException.
     *
     * @param exceptionType the exception expected to be called during the call
     * @param message       the expected message of the exception
     * @param when          the method to be called
     * @param <E>           the type of the inferred exception
     */
    public static <E> void expectException(Class<E> exceptionType,
                                           String message,
                                           Action when) {
        expectException(exceptionType, message, when, false);
    }
    
    public static <E> void expectExceptionCause(Class<E> exceptionType,
                                                String message,
                                                Action when) {
        expectException(exceptionType, message, when, true);
    }
    
    private static <E> void expectException(Class<E> exceptionType,
                                            String message,
                                            Action when,
                                            boolean checkCause) {
        try {
            when.act();
        }
        catch (Exception e) {
            Throwable toCheck = checkCause ? e.getCause() : e;
            
            if (toCheck != null && exceptionType.equals(toCheck.getClass())) {
                assertEquals("The exception did not have the right message",
                             message,
                             toCheck.getMessage()
                );
                return;
            }
        }
        fail("Expected exception was not caught: " + exceptionType);
    }
    
    
    public static <T> Observable<T> getMockObservable(ArgumentCaptor<T> returnValueCaptor) {
        return new Observable<T>() {
            @Override
            protected void subscribeActual(Observer<? super T> observer) {
                observer.onNext(returnValueCaptor.capture());
            }
        };
    }
    
}
