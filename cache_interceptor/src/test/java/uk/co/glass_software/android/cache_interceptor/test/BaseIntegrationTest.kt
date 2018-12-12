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

package uk.co.glass_software.android.cache_interceptor.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.BuildConfig
import uk.co.glass_software.android.cache_interceptor.RxCache
import uk.co.glass_software.android.cache_interceptor.injection.DaggerIntegrationTestComponent
import uk.co.glass_software.android.cache_interceptor.injection.IntegrationTestConfigurationModule
import uk.co.glass_software.android.cache_interceptor.test.network.MockClient
import uk.co.glass_software.android.cache_interceptor.test.network.retrofit.TestClient
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory
import java.io.IOException
import javax.inject.Inject

private const val folder = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE

@RunWith(RobolectricTestRunner::class)
@Config(
//        manifest = "build/intermediates/manifests/aapt/$folder/AndroidManifest.xml",
//        resourceDir = "build/intermediates/res/merged/$folder",
//        assetDir = "build/intermediates/assets/$folder",
        packageName = BuildConfig.APPLICATION_ID
)
abstract class BaseIntegrationTest {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var retrofit: Retrofit

    @Inject
    lateinit var testClient: TestClient

    @Inject
    lateinit var mockClient: MockClient

    @Inject
    lateinit var assetHelper: AssetHelper

    init {
        DaggerIntegrationTestComponent.builder()
                .integrationTestConfigurationModule(IntegrationTestConfigurationModule())
                .build()
                .inject(this)
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

}

