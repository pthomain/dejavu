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

import com.nhaarman.mockitokotlin2.mock
import dev.pthomain.android.dejavu.di.DejaVuComponent
import dev.pthomain.android.dejavu.di.integration.component.IntegrationDejaVuComponent
import dev.pthomain.android.dejavu.di.integration.module.ASSETS_FOLDER
import dev.pthomain.android.dejavu.di.integration.module.BASE_URL
import dev.pthomain.android.dejavu.di.integration.module.NOW
import dev.pthomain.android.dejavu.di.integration.component.IntegrationTestComponent
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.CacheInstruction
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.PlainRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.error.DejaVuError
import dev.pthomain.android.dejavu.error.DejaVuErrorFactory
import dev.pthomain.android.dejavu.test.network.MockClient
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.test.network.retrofit.TestClient
import dev.pthomain.android.dejavu.utils.SilentLogger
import okhttp3.OkHttpClient
import org.junit.Before
import retrofit2.Retrofit
import java.io.IOException
import java.util.*

internal abstract class BaseIntegrationTest<T : Any>(
        private val targetExtractor: (IntegrationDejaVuComponent) -> T
) {

    protected val now = NOW

    protected lateinit var okHttpClient: OkHttpClient
    protected lateinit var retrofit: Retrofit
    protected lateinit var mockClient: MockClient
    protected lateinit var testClient: TestClient
    protected lateinit var assetHelper: AssetHelper

    protected lateinit var cacheComponent: IntegrationDejaVuComponent
    protected lateinit var target: T

    @Before
    open fun setUp() {
        cacheComponent = DejaVuComponent(
                mock(), // context
                SilentLogger(),
                DejaVuErrorFactory(),
                mock(), // persistenceManager
                emptyList(), // decorators
                { null }, // operationPredicate
                { null }, // durationPredicate
                { if (it == null) NOW else Date(it) } // dateFactory
        )

        val testComponent = IntegrationTestComponent(BASE_URL, ASSETS_FOLDER)

        okHttpClient = testComponent.okHttpClient
        retrofit = testComponent.retrofit
        mockClient = testComponent.mockClient
        testClient = testComponent.testClient
        assetHelper = testComponent.assetHelper

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

    protected fun instructionToken(operation: Cache = Cache(durationInSeconds = 3600),
                                   responseClass: Class<*> = TestResponse::class.java,
                                   url: String = "http://test.com/testResponse") =
            RequestToken(
                    CacheInstruction(
                            operation,
                            cacheComponent.hasher.hash(PlainRequestMetadata(responseClass, url)
                            ) as HashedRequestMetadata
                    ),
                    CacheStatus.INSTRUCTION,
                    now
            )
}
