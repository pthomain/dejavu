package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache;


import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import uk.co.glass_software.android.cache_interceptor.base.BaseIntegrationTest;
import uk.co.glass_software.android.cache_interceptor.base.network.model.TestResponse;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database.DatabaseManager;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ErrorCode;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.CACHED;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.COULD_NOT_REFRESH;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.DO_NOT_CACHE;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.FRESH;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.NOT_CACHED;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.REFRESHED;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.STALE;

public class CacheInterceptorIntegrationTest extends BaseIntegrationTest {
    
    private final String apiUrl = "http://test.com";
    private final String body = "someBody";
    private DatabaseManager databaseManager;
    private TestResponse cachedResponse;
    private TestResponse networkResponse;
    private CacheManager spyCacheManager;
    
    @Before
    public void setUp() throws Exception {
        databaseManager = dependencyHelper.databaseManager;
        cachedResponse = getResponse();
        networkResponse = getResponse();
        networkResponse.remove(0);//removing element to test an updated response
        
        spyCacheManager = spy(dependencyHelper.cacheManager);
    }
    
    private TestResponse getResponse() {
        return assetHelper.getStubbedResponse(TestResponse.STUB_FILE, TestResponse.class)
                          .doOnNext(response -> response.setMetadata(ResponseMetadata.create(getToken(false),
                                                                                             null
                          )))
                          .blockingFirst();
    }
    
    private CacheInterceptor<ApiError, TestResponse> getInterceptor(CacheManager cacheManager,
                                                                    boolean isCacheEnabled,
                                                                    boolean isDoNotCache,
                                                                    Function<ApiError, Boolean> isNetworkError) {
        return new CacheInterceptor<>(
                cacheManager,
                isCacheEnabled,
                mock(Logger.class),
                isNetworkError,
                getToken(isDoNotCache)
        );
    }
    
    private CacheToken<TestResponse> getToken(boolean isDoNotCache) {
        return isDoNotCache
               ? CacheToken.Companion.doNotCache(TestResponse.class)
               : CacheToken.Companion.newRequest(TestResponse.class, apiUrl, body, 5);
    }
    
    @Test
    public void testInterceptorNoCaching() throws Exception {
        testInterceptor(false,
                        false,
                        false,
                        false,
                        NOT_CACHED,
                        false
        );
    }
    
    @Test
    public void testInterceptorDoNotCacheToken() throws Exception {
        testInterceptor(true,
                        true,
                        false,
                        false,
                        NOT_CACHED,
                        false
        );
    }
    
    @Test
    public void testInterceptorWithFreshCachedResponse() throws Exception {
        testInterceptor(true,
                        false,
                        true,
                        false,
                        CACHED,
                        false
        );
    }
    
    @Test
    public void testInterceptorWithStaleCachedResponseNetworkError() throws Exception {
        testInterceptor(true,
                        false,
                        true,
                        true,
                        COULD_NOT_REFRESH,
                        true
        );
    }
    
    @Test
    public void testInterceptorWithStaleCachedResponseNoNetworkError() throws Exception {
        testInterceptor(true,
                        false,
                        true,
                        true,
                        REFRESHED,
                        false
        );
    }
    
    @Test
    public void testInterceptorNoCachedResponseNetworkError() throws Exception {
        testInterceptor(true,
                        false,
                        false,
                        false,
                        NOT_CACHED,
                        true
        );
    }
    
    @Test
    public void testInterceptorNoCachedResponseNoNetworkError() throws Exception {
        testInterceptor(true,
                        false,
                        false,
                        false,
                        FRESH,
                        false
        );
    }
    
    private void testInterceptor(boolean isCachingEnabled,
                                 boolean isDoNotCache,
                                 boolean hasCachedResponse,
                                 boolean isCachedResponseStale,
                                 CacheStatus expectedStatus,
                                 boolean isNetworkError) {
        CacheToken<TestResponse> existingCacheToken;
        
        
        if (!isCachingEnabled) { //this overrides default tokens to test the "isCachingEnabled" config only
            existingCacheToken = getToken(false);
        }
        else if (hasCachedResponse) {
            existingCacheToken = CacheToken.Companion.cached(getToken(false),
                                                   null,
                                                   new Date(),
                                                   new Date(System.currentTimeMillis() + 5000 * 60)
            );
        }
        else if (isNetworkError || isDoNotCache) {
            existingCacheToken = getToken(true);
        }
        else {
            existingCacheToken = getToken(false);
        }
        
        if (hasCachedResponse) {
            DatabaseManagerIntegrationTest.cache(databaseManager, cachedResponse, existingCacheToken);
            
            assertNotNull("Response should have been cached",
                          DatabaseManagerIntegrationTest.getCachedResponse(databaseManager, null, existingCacheToken)
            );
        }
        else {
            assertNull("Response should not be cached", DatabaseManagerIntegrationTest.getCachedResponse(databaseManager, null, existingCacheToken));
        }
        
        TestResponse errorResponse = new TestResponse();
        errorResponse.setMetadata(ResponseMetadata.create(
                getToken(true),
                new ApiError(new IOException("test"), 404, ErrorCode.NOT_FOUND, "some Exception")
        ));
        
        TestResponse expectedResponse;
        if (expectedStatus == FRESH || expectedStatus == REFRESHED) {
            expectedResponse = networkResponse;
        }
        else if (expectedStatus == NOT_CACHED) {
            if (isNetworkError) {
                expectedResponse = errorResponse;
            }
            else {
                expectedResponse = networkResponse;
            }
        }
        else if (hasCachedResponse && (expectedStatus == CACHED || expectedStatus == STALE || expectedStatus == COULD_NOT_REFRESH)) {
            expectedResponse = cachedResponse;
        }
        else {
            expectedResponse = errorResponse;
        }
        
        if (isCachedResponseStale) {
            DatabaseManagerIntegrationTest.prepareSpyCacheManagerToReturnStale(spyCacheManager);
        }
        
        CacheInterceptor<ApiError, TestResponse> interceptor = getInterceptor(spyCacheManager,
                                                                              isCachingEnabled,
                                                                              isDoNotCache,
                                                                              ignore -> isNetworkError
        );
        
        Observable<TestResponse> observable = Observable.just(isNetworkError ? errorResponse : networkResponse);
        
        if (isCachedResponseStale) {
            List<TestResponse> testResponses = observable.compose(interceptor).toList().blockingGet();
            
            assertResponse(testResponses.get(0), cachedResponse, STALE);
            assertResponse(testResponses.get(1),
                           isNetworkError ? cachedResponse : networkResponse,
                           isNetworkError ? COULD_NOT_REFRESH : REFRESHED
            );
        }
        else {
            assertResponse(observable.compose(interceptor).blockingFirst(), expectedResponse, expectedStatus);
        }
    }
    
    private void assertResponse(TestResponse testResponse,
                                TestResponse expectedResponse,
                                CacheStatus expectedStatus) {
        assertEquals("Responses didn't match", expectedResponse, testResponse);
        
        ResponseMetadata<TestResponse, ApiError> metadata = expectedResponse.getMetadata();
        if (metadata.hasError()) {
            assertNotNull("Response should have an error", metadata.getError());
            assertEquals("Errors did not match", metadata.getError(), testResponse.getMetadata().getError());
        }
        else {
            assertNull("Response should have no error", testResponse.getMetadata().getError());
        }
        
        CacheToken<?> cacheToken = testResponse.getMetadata().getCacheToken();
        
        assertEquals("Response class didn't match", TestResponse.class, cacheToken.getResponseClass());
        
        if (cacheToken.getStatus() != NOT_CACHED
            && cacheToken.getStatus() != DO_NOT_CACHE) {
            assertEquals("Cache token url didn't match", apiUrl, cacheToken.getApiUrl());
            assertEquals("Body didn't match", body, cacheToken.getBody());
            assertNotNull("Fetch date should not be null", cacheToken.getFetchDate());
            assertNotNull("Cache date should not be null", cacheToken.getCacheDate());
            assertNotNull("Expiry date should not be null", cacheToken.getExpiryDate());
        }
        else {
            assertNotNull("Fetch date should not be null", cacheToken.getFetchDate());
            assertNull("Cache date should not be null", cacheToken.getCacheDate());
            assertNull("Expiry date should not be null", cacheToken.getExpiryDate());
        }
        
        assertEquals("Cache token should be " + expectedStatus, expectedStatus, cacheToken.getStatus());
    }
}