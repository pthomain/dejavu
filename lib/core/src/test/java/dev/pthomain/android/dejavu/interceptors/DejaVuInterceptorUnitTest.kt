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

package dev.pthomain.android.dejavu.interceptors

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.dejavu.cache.metadata.response.CallDuration
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.FRESH
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.INSTRUCTION
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.CacheInstruction
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.INVALID_HASH
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.PlainRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.error.DejaVuError
import dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
import dev.pthomain.android.dejavu.serialisation.SerialisationArgumentValidator
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.utils.SilentLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class DejaVuInterceptorUnitTest {

    private val now = Date(1234L)

    @Test
    fun `interceptor composes network, cache, and response interceptors for Cache operation`() = runTest {
        val hashedMetadata = HashedRequestMetadata(
                TestResponse::class.java,
                "http://test.com/test",
                null,
                "testHash",
                "testClassHash"
        )

        val instruction = CacheInstruction<Cache, TestResponse>(
                Cache(durationInSeconds = 3600),
                hashedMetadata
        )

        val expectedResponse = Response<TestResponse, Cache>(
                TestResponse(),
                ResponseToken(instruction, FRESH, now),
                CallDuration(0, 0, 0)
        )

        // Mock the network interceptor to pass through
        val mockNetworkInterceptorFactory = mock<NetworkInterceptor.Factory<DejaVuError>>()
        val mockNetworkInterceptor = mock<NetworkInterceptor<Cache, TestResponse, RequestToken<out Cache, TestResponse>, DejaVuError>>()
        whenever(mockNetworkInterceptorFactory.create<Cache, TestResponse, RequestToken<out Cache, TestResponse>>(any()))
                .thenReturn(mockNetworkInterceptor)
        whenever(mockNetworkInterceptor.intercept(any<Flow<DejaVuResult<TestResponse>>>()))
                .thenReturn(flowOf(expectedResponse))

        // Mock cache interceptor to pass through
        val mockCacheInterceptorFactory = mock<CacheInterceptor.Factory<DejaVuError>>()
        val mockCacheInterceptor = mock<CacheInterceptor<TestResponse, Cache, DejaVuError>>()
        whenever(mockCacheInterceptorFactory.create<TestResponse, Cache>(any()))
                .thenReturn(mockCacheInterceptor)
        whenever(mockCacheInterceptor.intercept(any()))
                .thenReturn(flowOf(expectedResponse))

        // Mock response interceptor to pass through as result wrapper
        val mockResponseInterceptorFactory = mock<ResponseInterceptor.Factory<DejaVuError>>()
        val mockResponseInterceptor = mock<ResponseInterceptor<TestResponse, DejaVuError>>()
        whenever(mockResponseInterceptorFactory.create<TestResponse>(any()))
                .thenReturn(mockResponseInterceptor)
        whenever(mockResponseInterceptor.intercept(any()))
                .thenReturn(flowOf(expectedResponse as Any))

        // Create the hasher that returns a known hash
        val mockHasher = mock<dev.pthomain.android.dejavu.cache.metadata.token.instruction.Hasher>()
        whenever(mockHasher.hash(any<PlainRequestMetadata<TestResponse>>()))
                .thenReturn(hashedMetadata)

        val interceptor = DejaVuInterceptor.Factory<DejaVuError>(
                mockHasher,
                SilentLogger,
                { if (it == null) now else Date(it) },
                SerialisationArgumentValidator(emptyList()),
                mockNetworkInterceptorFactory,
                mockCacheInterceptorFactory,
                mockResponseInterceptorFactory
        ).create<TestResponse>(
                asResult = true,
                operation = Cache(durationInSeconds = 3600),
                requestMetadata = PlainRequestMetadata(TestResponse::class.java, "http://test.com/test")
        )

        val upstream = flowOf(mock<Any>())
        val results = interceptor.intercept(upstream).toList()

        assertTrue("Should produce at least one result", results.isNotEmpty())
    }

    @Test
    fun `interceptor emits error flow when hashing fails`() = runTest {
        val mockHasher = mock<dev.pthomain.android.dejavu.cache.metadata.token.instruction.Hasher>()
        whenever(mockHasher.hash(any<PlainRequestMetadata<TestResponse>>()))
                .thenReturn(null)

        val interceptor = DejaVuInterceptor.Factory<DejaVuError>(
                mockHasher,
                SilentLogger,
                { if (it == null) now else Date(it) },
                SerialisationArgumentValidator(emptyList()),
                mock(),
                mock(),
                mock()
        ).create<TestResponse>(
                asResult = true,
                operation = Cache(durationInSeconds = 3600),
                requestMetadata = PlainRequestMetadata(TestResponse::class.java, "http://test.com/test")
        )

        val upstream = flowOf(mock<Any>())

        try {
            interceptor.intercept(upstream).toList()
            assertTrue("Should have thrown an exception", false)
        } catch (e: IllegalStateException) {
            assertTrue("Expected hashing error message",
                    e.message?.contains("could not be hashed") == true)
        }
    }
}
