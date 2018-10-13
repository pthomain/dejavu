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

package uk.co.glass_software.android.cache_interceptor.interceptors.internal.error;


import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import io.reactivex.Observable;
import uk.co.glass_software.android.boilerplate.utils.log.Logger;
import uk.co.glass_software.android.cache_interceptor.base.BaseIntegrationTest;
import uk.co.glass_software.android.cache_interceptor.base.network.model.TestResponse;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.DO_NOT_CACHE;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiErrorFactoryUnitTest.assertApiError;
import static uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ErrorCode.NETWORK;

@SuppressWarnings("unchecked")
public class ErrorInterceptorIntegrationTest extends BaseIntegrationTest {
    
    private final String message = "Connection timed out";
    
    private ErrorInterceptor<TestResponse, ApiError> target;
    
    @Before
    public void setUp() throws Exception {
        CacheToken<TestResponse> cacheToken = CacheToken.Companion.newRequest(
                TestResponse.class,
                TestResponse.Companion.getURL(),
                null,
                5f
        );
        
        target = new ErrorInterceptor.Factory<>(
                new ApiErrorFactory(),
                mock(Logger.class)
        ).create(cacheToken);
    }
    
    @Test
    public void testInterceptorWithError() throws Exception {
        testInterceptor(true);
    }
    
    @Test
    public void testInterceptorNoError() throws Exception {
        testInterceptor(false);
    }
    
    private void testInterceptor(boolean hasError) throws Exception {
        TestResponse successResponse = new TestResponse();
        Observable<TestResponse> observable = Observable.just(successResponse);
        
        if (hasError) {
            observable = observable.doOnSubscribe(ignore -> {
                throw new IOException(message);
            });
        }
        
        TestResponse response = observable.compose(target).blockingFirst();
        
        ResponseMetadata<TestResponse, ApiError> metadata = response.getMetadata();
        assertNotNull("TestResponse should have metadata", metadata);
        
        if (hasError) {
            Companion.assertApiError(
                    metadata.getError(),
                    "Connection timed out",
                    NETWORK,
                    -1,
                    true
            );
            
            assertEquals("Cache token should be DoNotCache",
                         DO_NOT_CACHE,
                         metadata.getCacheToken().getStatus()
            );
        }
        else {
            assertFalse("PotListResponse should not have errors", metadata.hasError());
            assertEquals("Responses didn't match", successResponse, response);
        }
    }
    
}