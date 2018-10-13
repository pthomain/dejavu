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

package uk.co.glass_software.android.cache_interceptor.base;

import android.app.Application;

import com.google.gson.Gson;

import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.IOException;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import uk.co.glass_software.android.boilerplate.log.Logger;
import uk.co.glass_software.android.cache_interceptor.BuildConfig;
import uk.co.glass_software.android.cache_interceptor.base.network.MockClient;
import uk.co.glass_software.android.cache_interceptor.base.network.retrofit.TestClient;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.CacheInterceptorBuilderHelper;
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError;
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(CustomRobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public abstract class BaseIntegrationTest {
    
    public static final String ASSETS_FOLDER = "src/main/assets/";
    public static final String BASE_URL = "http://test.com";
    
    protected final Application application;
    private final OkHttpClient okHttpClient;
    private final Retrofit retrofit;
    
    protected final TestClient testClient;
    protected final AssetHelper assetHelper;
    private final RetrofitCacheAdapterFactory<ApiError> cacheFactory;
    protected final CacheInterceptorBuilderHelper dependencyHelper;
    
    private MockClient mockClient;
    
    protected BaseIntegrationTest() {
        application = spy(RuntimeEnvironment.application);
        mockClient = new MockClient();
        
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(mockClient);
        okHttpClient = builder.build();
        
        Gson gson = new Gson();
        cacheFactory = RetrofitCacheAdapterFactory.Companion.buildDefault(application);
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(cacheFactory)
                .build();
        
        testClient = retrofit.create(TestClient.class);
        assetHelper = new AssetHelper(
                ASSETS_FOLDER,
                gson,
                mock(Logger.class)
        );
    
        dependencyHelper = new CacheInterceptorBuilderHelper(RuntimeEnvironment.application);
    }
    
    protected void enqueueResponse(String response,
                                   int httpCode) {
        mockClient.enqueueResponse(response, httpCode);
    }
    
    public void enqueueRuntimeException(RuntimeException exception) {
        mockClient.enqueueRuntimeException(exception);
    }
    
    public void enqueueIOException(IOException exception) {
        mockClient.enqueueIOException(exception);
    }
    
}

