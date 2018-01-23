package uk.co.glass_software.android.cache_interceptor.interceptors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.reactivex.Observable;
import uk.co.glass_software.android.cache_interceptor.base.network.model.TestResponse;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken;
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheTokenHelper;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor;
import uk.co.glass_software.android.cache_interceptor.utils.Function;
import uk.co.glass_software.android.cache_interceptor.utils.Logger;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken.Status.CACHE;
import static uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken.Status.REFRESH;

@SuppressWarnings("unchecked")
public class RxCacheInterceptorUnitTest {
    
    private Class<TestResponse> responseClass;
    private final String mockUrl = "mockUrl";
    private final String mockBody = "mockBody";
    private Logger mockLogger;
    private ErrorInterceptor.Factory<ApiError> mockErrorInterceptorFactory;
    private CacheInterceptor.Factory<ApiError> mockCacheInterceptorFactory;
    private ErrorInterceptor<TestResponse, ApiError> mockErrorInterceptor;
    private CacheInterceptor<ApiError, TestResponse> mockCacheInterceptor;
    private Observable<TestResponse> mockObservable;
    
    @Before
    public void setUp() throws Exception {
        responseClass = TestResponse.class;
        mockLogger = mock(Logger.class);
        mockErrorInterceptorFactory = mock(ErrorInterceptor.Factory.class);
        mockCacheInterceptorFactory = mock(CacheInterceptor.Factory.class);
        mockErrorInterceptor = mock(ErrorInterceptor.class);
        mockCacheInterceptor = mock(CacheInterceptor.class);
        mockObservable = Observable.just(mock(TestResponse.class));
        
        when(mockErrorInterceptor.apply(any())).thenReturn(mockObservable);
        when(mockCacheInterceptor.apply(any())).thenReturn(mockObservable);
        
        when(mockErrorInterceptorFactory.<TestResponse>create(any())).thenReturn(mockErrorInterceptor);
        when(mockCacheInterceptorFactory.<TestResponse>create(any(), any())).thenReturn(mockCacheInterceptor);
    }
    
    private RxCacheInterceptor getTarget(boolean isRefresh) {
        return new RxCacheInterceptor(
                responseClass,
                mockUrl,
                mockBody,
                isRefresh,
                mockLogger,
                mockErrorInterceptorFactory,
                mockCacheInterceptorFactory
        );
    }
    
    @Test
    public void testApplyIsRefreshTrue() throws Exception {
        testApply(true);
    }
    
    @Test
    public void testApplyIsRefreshFalse() throws Exception {
        testApply(false);
    }
    
    private void testApply(boolean isRefresh) throws Exception {
        getTarget(isRefresh).apply(mockObservable);
        
        ArgumentCaptor<CacheToken> errorTokenCaptor = ArgumentCaptor.forClass(CacheToken.class);
        ArgumentCaptor<CacheToken> cacheTokenCaptor = ArgumentCaptor.forClass(CacheToken.class);
        
        verify(mockErrorInterceptorFactory).create(errorTokenCaptor.capture());
        verify(mockCacheInterceptorFactory).create(cacheTokenCaptor.capture(), (Function<ApiError, Boolean>) any(Function.class));
        
        CacheToken errorToken = errorTokenCaptor.getValue();
        CacheToken cacheToken = cacheTokenCaptor.getValue();
        
        assertEquals(errorToken, cacheToken);
        
        CacheTokenHelper.verifyCacheToken(
                errorToken,
                mockUrl,
                mockBody,
                "4529567321185990290",
                null,
                null,
                null,
                null,
                TestResponse.class,
                isRefresh ? REFRESH : CACHE,
                5f
        );
    }
}