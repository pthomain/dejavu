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
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Cache
import uk.co.glass_software.android.dejavu.injection.integration.component.DaggerIntegrationCacheComponent
import uk.co.glass_software.android.dejavu.injection.integration.component.DaggerIntegrationTestComponent
import uk.co.glass_software.android.dejavu.injection.integration.component.IntegrationCacheComponent
import uk.co.glass_software.android.dejavu.injection.integration.module.IntegrationCacheModule
import uk.co.glass_software.android.dejavu.injection.integration.module.IntegrationTestModule
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.RequestMetadata
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken.Companion.fromInstruction
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.interceptors.internal.error.GlitchFactory
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.test.network.MockClient
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import uk.co.glass_software.android.dejavu.test.network.model.User
import uk.co.glass_software.android.dejavu.test.network.retrofit.TestClient
import uk.co.glass_software.android.mumbo.Mumbo
import java.io.IOException
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(packageName = BuildConfig.APPLICATION_ID)
internal abstract class BaseIntegrationTest<T>(targetExtractor: (IntegrationCacheComponent) -> T) {

    protected val okHttpClient: OkHttpClient
    protected val retrofit: Retrofit
    protected val mockClient: MockClient
    protected val testClient: TestClient
    protected val assetHelper: AssetHelper

    protected val NOW = Date(1234L)

    protected val configuration = CacheConfiguration(
            ApplicationProvider.getApplicationContext(),
            mock(),
            GlitchFactory(),
            GsonSerialiser(Gson()),
            Mumbo(ApplicationProvider.getApplicationContext()).conceal(),
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

    protected fun enqueueResponse(response: String,
                                  httpCode: Int) {
        mockClient.enqueueResponse(response, httpCode)
    }

    protected fun enqueueRuntimeException(exception: RuntimeException) {
        mockClient.enqueueRuntimeException(exception)
    }

    protected fun enqueueIOException(exception: IOException) {
        mockClient.enqueueIOException(exception)
    }

    protected fun getStubbedTestResponse(instructionToken: CacheToken = instructionToken()) =
            assetHelper.observeStubbedResponse(
                    TestResponse.STUB_FILE,
                    TestResponse::class.java,
                    instructionToken
            ).blockingFirst().let {
                ResponseWrapper(
                        TestResponse::class.java,
                        it,
                        it.metadata
                )
            }

    protected fun getStubbedUserResponseWrapper(instructionToken: CacheToken = instructionToken(),
                                                url: String = "http://test.com/userResponse") =
            getStubbedTestResponse(instructionToken).let {
                with(it.metadata) {
                    ResponseWrapper(
                            User::class.java,
                            (it.response as TestResponse).first(),
                            copy(
                                    cacheToken = cacheToken.copy(
                                            cacheToken.instruction.copy(
                                                    responseClass = User::class.java
                                            ),
                                            requestMetadata = cacheComponent.hasher().hash(
                                                    RequestMetadata.UnHashed(url)
                                            )
                                    )
                            )
                    )
                }
            }

    protected fun assertResponse(
            stubbedResponse: ResponseWrapper<Glitch>,
            actualResponse: ResponseWrapper<Glitch>?,
            expectedStatus: CacheStatus,
            fetchDate: Date? = NOW,
            cacheDate: Date? = NOW,
            expiryDate: Date? = Date(NOW.time + (stubbedResponse.metadata.cacheToken.instruction.operation as Operation.Expiring).durationInMillis!!)
    ) {
        assertNotNullWithContext(
                actualResponse,
                "Actual response should not be null"
        )

        assertEqualsWithContext(
                stubbedResponse.response,
                actualResponse!!.response,
                "Response didn't match"
        )

        assertEqualsWithContext(
                CacheToken(
                        stubbedResponse.metadata.cacheToken.instruction,
                        expectedStatus,
                        configuration.compress,
                        configuration.encrypt,
                        stubbedResponse.metadata.cacheToken.requestMetadata,
                        fetchDate,
                        cacheDate,
                        expiryDate
                ),
                actualResponse.metadata.cacheToken,
                "Cache token didn't match"
        )
    }

    protected fun instructionToken(operation: Operation = Cache(durationInMillis = 3600_000),
                                   responseClass : Class<*> = TestResponse::class.java,
                                   url: String = "http://test.com/testResponse") = fromInstruction(
            CacheInstruction(
                    responseClass,
                    operation
            ),
            true,
            true,
            cacheComponent.hasher().hash(
                    RequestMetadata.UnHashed(url)
            )
    )

}

