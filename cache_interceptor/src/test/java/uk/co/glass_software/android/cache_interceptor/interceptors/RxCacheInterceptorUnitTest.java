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

package uk.co.glass_software.android.cache_interceptor.interceptors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.reactivex.Observable;
import uk.co.glass_software.android.cache_interceptor.base.network.model.TestResponse;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.CacheInterceptor;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.CacheTokenHelper;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ErrorInterceptor;
import uk.co.glass_software.android.cache_interceptor.utils.Function;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.CACHE;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.REFRESH;

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
                true,
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