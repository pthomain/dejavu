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

package uk.co.glass_software.android.dejavu.test

import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.mock
import okhttp3.OkHttpClient
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import uk.co.glass_software.android.dejavu.BuildConfig
import uk.co.glass_software.android.dejavu.DejaVu
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.injection.integration.component.DaggerIntegrationCacheComponent
import uk.co.glass_software.android.dejavu.injection.integration.component.DaggerIntegrationTestComponent
import uk.co.glass_software.android.dejavu.injection.integration.component.IntegrationCacheComponent
import uk.co.glass_software.android.dejavu.injection.integration.module.IntegrationCacheModule
import uk.co.glass_software.android.dejavu.injection.integration.module.IntegrationTestModule
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.GlitchFactory
import uk.co.glass_software.android.dejavu.test.network.MockClient
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import uk.co.glass_software.android.dejavu.test.network.retrofit.TestClient
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(packageName = BuildConfig.APPLICATION_ID)
internal abstract class BaseIntegrationTest<T>(targetExtractor: (IntegrationCacheComponent) -> T) {

    protected val okHttpClient: OkHttpClient
    protected val retrofit: Retrofit
    protected val mockClient: MockClient
    protected val testClient: TestClient
    protected val assetHelper: AssetHelper

    private val configuration = CacheConfiguration(
            ApplicationProvider.getApplicationContext(),
            mock(),
            GlitchFactory(),
            Gson(),
            true,
            true,
            true,
            true,
            false,
            15,
            15,
            60000,
            false
    )

    protected val cacheComponent: IntegrationCacheComponent = DaggerIntegrationCacheComponent.builder()
            .integrationCacheModule(IntegrationCacheModule(configuration))
            .build()

    private val dejaVu = DejaVu(cacheComponent)

    protected val target: T

    init {
        val testComponent = DaggerIntegrationTestComponent.builder()
                .integrationTestModule(IntegrationTestModule(dejaVu))
                .build()

        okHttpClient = testComponent.okHttpClient()
        retrofit = testComponent.retrofit()
        mockClient = testComponent.mockClient()
        testClient = testComponent.testClient()
        assetHelper = testComponent.assetHelper()


        target = targetExtractor(cacheComponent)
    }

    protected fun instructionToken(operation: CacheInstruction.Operation) = CacheToken.fromInstruction(
            CacheInstruction(
                    TestResponse::class.java,
                    operation
            ),
            true,
            true,
            "/",
            null
    )

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

