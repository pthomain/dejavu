/*
 *
 *  Copyright (C) 2017-2020 Pierre Thomain
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package dev.pthomain.android.dejavu.test

import androidx.annotation.CallSuper
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.mock
import dev.pthomain.android.dejavu.BuildConfig
import dev.pthomain.android.dejavu.DejaVu
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.injection.integration.component.DaggerIntegrationDejaVuComponent
import dev.pthomain.android.dejavu.injection.integration.component.DaggerIntegrationTestComponent
import dev.pthomain.android.dejavu.injection.integration.component.IntegrationDejaVuComponent
import dev.pthomain.android.dejavu.injection.integration.module.IntegrationModule
import dev.pthomain.android.dejavu.injection.integration.module.IntegrationTestModule
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CacheInstruction
import dev.pthomain.android.dejavu.interceptors.cache.instruction.PlainRequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.instruction.ValidRequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.test.network.MockClient
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.test.network.model.User
import dev.pthomain.android.dejavu.test.network.retrofit.TestClient
import dev.pthomain.android.glitchy.interceptor.error.glitch.Glitch
import dev.pthomain.android.glitchy.interceptor.error.glitch.GlitchFactory
import dev.pthomain.android.mumbo.Mumbo
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import java.io.IOException
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(packageName = BuildConfig.LIBRARY_PACKAGE_NAME)
internal abstract class BaseIntegrationTest<T : Any>(
        private val targetExtractor: (IntegrationDejaVuComponent) -> T,
        private val useDefaultConfiguration: Boolean = true
) {

    protected val NOW = Date(1234L)

    protected lateinit var okHttpClient: OkHttpClient
    protected lateinit var retrofit: Retrofit
    protected lateinit var mockClient: MockClient
    protected lateinit var testClient: TestClient
    protected lateinit var assetHelper: AssetHelper

    protected lateinit var cacheComponent: IntegrationDejaVuComponent
    protected lateinit var target: T

    private lateinit var dejaVu: DejaVu<Glitch>

    protected open val configuration = DejaVuConfiguration(
            ApplicationProvider.getApplicationContext(),
            mock(),
            GlitchFactory(),
            GsonSerialiser(Gson()),
            Mumbo(ApplicationProvider.getApplicationContext()).conceal(),
            true,
            null,
            { _ -> null },
            { _ -> null }
    )

    @Before
    @CallSuper
    open fun setUp() {
        if (useDefaultConfiguration) {
            setUpWithConfiguration(configuration)
        }
    }

    protected fun setUpWithConfiguration(configuration: DejaVuConfiguration<Glitch>) {
        cacheComponent = DaggerIntegrationDejaVuComponent.builder()
                .integrationDejaVuModule(IntegrationModule(configuration))
                .build()

        dejaVu = DejaVu(cacheComponent)

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

    protected fun getStubbedTestResponse(instructionToken: RequestToken<Cache, TestResponse> = instructionToken()) =
            assetHelper.observeStubbedResponse(
                    TestResponse.STUB_FILE,
                    TestResponse::class.java,
                    instructionToken
            ).blockingFirst()

    protected fun getStubbedUserResponseWrapper(
            instructionToken: RequestToken<Cache, User> = instructionToken(responseClass = User::class.java),
            url: String = "http://test.com/userResponse"
    ) =
            assetHelper.observeStubbedResponse(
                    UserResponse.STUB_FILE,
                    TestResponse::class.java,
                    instructionToken
            ).blockingFirst()
                    .let {
                        val requestMetadata = instructionToken.instruction.requestMetadata

                        with(it.metadata) {
                            ResponseWrapper(
                                    requestMetadata.responseClass,
                                    (it.response as TestResponse).first(),
                                    copy(
                                            cacheToken = cacheToken.copy(
                                                    CacheInstruction(
                                                            cacheComponent.hasher().hash(
                                                                    RequestMetadata.Plain(
                                                                            requestMetadata.responseClass,
                                                                            url
                                                                    )
                                                            ) as ValidRequestMetadata,
                                                            cacheToken.instruction.operation
                                                    )
                                            )
                                    )
                            )
                }
            }

    protected fun assertResponse(stubbedResponse: ResponseWrapper<Cache, ResponseToken<Cache>, Glitch>,
                                 actualResponse: ResponseWrapper<*, *, Glitch>?,
                                 expectedStatus: CacheStatus,
                                 fetchDate: Date = NOW,
                                 cacheDate: Date? = NOW,
                                 expiryDate: Date? = Date(NOW.time + stubbedResponse.metadata.cacheToken.instruction.operation.durationInSeconds * 1000)) {
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
                ResponseToken(
                        stubbedResponse.metadata.cacheToken.instruction,
                        expectedStatus,
                        fetchDate,
                        cacheDate,
                        expiryDate
                ),
                actualResponse.metadata.cacheToken,
                "Cache token didn't match"
        )
    }

    protected fun instructionToken(operation: Cache = Cache(durationInSeconds = 3600),
                                   responseClass: Class<*> = TestResponse::class.java,
                                   url: String = "http://test.com/testResponse") =
            InstructionToken(
                    CacheInstruction(
                            operation,
                            cacheComponent.hasher().hash(PlainRequestMetadata(responseClass, url)
                            ) as ValidRequestMetadata
                    )
            )
}

