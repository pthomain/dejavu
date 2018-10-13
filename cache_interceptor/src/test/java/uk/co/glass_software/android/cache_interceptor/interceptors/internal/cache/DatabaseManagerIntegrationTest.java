/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import io.reactivex.Observable;
import uk.co.glass_software.android.cache_interceptor.base.BaseIntegrationTest;
import uk.co.glass_software.android.cache_interceptor.base.network.model.TestResponse;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database.DatabaseManager;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.STALE;

@SuppressWarnings("unchecked")
public class DatabaseManagerIntegrationTest extends BaseIntegrationTest {
    
    private TestResponse stubbedResponse;
    private CacheToken<TestResponse> cacheToken;
    private Observable<TestResponse> mockUpstream;

    private DatabaseManager target;

    @Before
    public void setUp() throws Exception {
        CacheToken requestToken = CacheToken.Companion.newRequest(
                TestResponse.class,
                "apiUrl",
                "",
                5
        );

        stubbedResponse = assetHelper.getStubbedResponse(TestResponse.STUB_FILE, TestResponse.class).blockingFirst();
        stubbedResponse.setMetadata(ResponseMetadata.create(requestToken, null));

        mockUpstream = mock(Observable.class);

        Date fetchDate = new Date();
        Date expiryDate = new Date(fetchDate.getTime() + 12345);

        cacheToken = CacheToken.Companion.caching(requestToken,
                                        mockUpstream,
                                        fetchDate,
                                        fetchDate,
                                        expiryDate
        );

        stubbedResponse.getMetadata().setCacheToken(cacheToken);

        target = dependencyHelper.databaseManager;
    }

    @Test
    public void testCacheAndFlush() throws Exception {
        assertNull("Cache should not contain anything", target.getCachedResponse(mockUpstream, cacheToken));

        target.cache(stubbedResponse);

        TestResponse cachedResponse = target.getCachedResponse(mockUpstream, cacheToken);
        cachedResponse.setMetadata(stubbedResponse.getMetadata()); //ignore metadata, covered by unit test
        assertEquals("Cached response didn't match the original", stubbedResponse, cachedResponse);

        target.clearCache();

        assertNull("Cache should not contain anything", target.getCachedResponse(mockUpstream, cacheToken));
    }

    //used by other tests to preserve encapsulation
    @VisibleForTesting
    public static <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> void cache(DatabaseManager databaseManager,
                                                                                                                   R response,
                                                                                                                   CacheToken cacheToken) {
        response.setMetadata(ResponseMetadata.create(cacheToken, null));
        databaseManager.cache(response);
    }

    //used by other tests to preserve encapsulation
    @VisibleForTesting
    public static <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> R getCachedResponse(DatabaseManager databaseManager,
                                                                                                                            Observable<R> upstream,
                                                                                                                            CacheToken cacheToken) {
        return databaseManager.getCachedResponse(upstream, cacheToken);
    }

    //used by other tests to preserve encapsulation
    @VisibleForTesting
    public static <E extends Exception & Function<E, Boolean>, R extends ResponseMetadata.Holder<R, E>> CacheToken<R> getCachedCacheToken(CacheToken<R> cacheToken,
                                                                                                                                          @NonNull Observable<R> refreshObservable,
                                                                                                                                          @NonNull Date cacheDate,
                                                                                                                                          @NonNull Date expiryDate) {
        return CacheToken.Companion.cached(cacheToken,
                                 refreshObservable,
                                 cacheDate,
                                 expiryDate
        );
    }
    //used by other tests to preserve encapsulation
    @VisibleForTesting
    public static void prepareSpyCacheManagerToReturnStale(CacheManager spyCacheManager) {
        doReturn(STALE).when(spyCacheManager).getCachedStatus(any());
    }
}