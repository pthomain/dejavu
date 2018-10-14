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

package uk.co.glass_software.android.cache_interceptor.base

import android.app.Application
import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.BuildConfig
import uk.co.glass_software.android.cache_interceptor.RxCache
import uk.co.glass_software.android.cache_interceptor.base.network.MockClient
import uk.co.glass_software.android.cache_interceptor.base.network.retrofit.TestClient
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory
import java.io.IOException

@RunWith(CustomRobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
abstract class BaseIntegrationTest protected constructor() {

    protected val application: Application = spy(RuntimeEnvironment.application)
    private val okHttpClient: OkHttpClient
    private val retrofit: Retrofit

    protected val testClient: TestClient
    protected val assetHelper: AssetHelper

    private val cacheFactory: RetrofitCacheAdapterFactory<ApiError>
    protected val dependencyHelper: CacheInterceptorBuilderHelper

    private val mockClient: MockClient = MockClient()

    init {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(mockClient)
        okHttpClient = builder.build()

        val gson = Gson()

        cacheFactory = RxCache.builder()
                .gson(gson)
                .build(application)
                .retrofitCacheAdapterFactory

        retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(cacheFactory)
                .build()

        testClient = retrofit.create(TestClient::class.java)
        assetHelper = AssetHelper(
                ASSETS_FOLDER,
                gson,
                mock(Logger::class.java)
        )

        dependencyHelper = CacheInterceptorBuilderHelper(RuntimeEnvironment.application)
    }

    protected fun enqueueResponse(response: String,
                                  httpCode: Int) {
        mockClient.enqueueResponse(response, httpCode)
    }

    fun enqueueRuntimeException(exception: RuntimeException) {
        mockClient.enqueueRuntimeException(exception)
    }

    fun enqueueIOException(exception: IOException) {
        mockClient.enqueueIOException(exception)
    }

    companion object {

        val ASSETS_FOLDER = "src/main/assets/"
        val BASE_URL = "http://test.com"
    }

}

