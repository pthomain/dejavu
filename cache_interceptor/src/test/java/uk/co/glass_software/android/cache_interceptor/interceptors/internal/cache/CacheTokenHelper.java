package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache;

import java.util.Date;

import io.reactivex.Observable;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation.Hasher;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken;

import static junit.framework.Assert.assertEquals;

public class CacheTokenHelper {
    
    public static void verifyCacheToken(CacheToken cacheToken,
                                        String expectedUrl,
                                        String expectedBody,
                                        String expectedKey,
                                        Date cacheDate,
                                        Date expiryDate,
                                        Date fetchDate,
                                        Observable expectedObservable,
                                        Class expectedResponseClass,
                                        CacheStatus expectedStatus,
                                        float expectedTtl) {
        assertEquals("CacheToken URL didn't match", expectedUrl, cacheToken.getApiUrl());
        assertEquals("CacheToken body didn't match", expectedBody, cacheToken.getBody());
        assertEquals("CacheToken key didn't match", expectedKey, cacheToken.getKey(new Hasher(null)));
        assertEquals("CacheToken cached date didn't match", cacheDate, cacheToken.getCacheDate());
        assertEquals("CacheToken expiry date didn't match", expiryDate, cacheToken.getExpiryDate());
        assertEquals("CacheToken fetch date didn't match", fetchDate, cacheToken.getFetchDate());
        assertEquals("CacheToken refresh observable didn't match", expectedObservable, cacheToken.getRefreshObservable());
        assertEquals("CacheToken response class didn't match", expectedResponseClass, cacheToken.getResponseClass());
        assertEquals("CacheToken status didn't match", expectedStatus, cacheToken.getStatus());
        assertEquals("CacheToken TTL didn't match", expectedTtl, cacheToken.getTtlInMinutes());
    }
}
