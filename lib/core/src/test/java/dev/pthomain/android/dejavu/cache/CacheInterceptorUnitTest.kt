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

package dev.pthomain.android.dejavu.cache

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.dejavu.cache.metadata.response.CallDuration
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.response.Result
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.*
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.CacheInstruction
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.INVALID_HASH
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Invalidate
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.DoNotCache
import dev.pthomain.android.dejavu.error.DejaVuError
import dev.pthomain.android.dejavu.interceptors.CacheInterceptor
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class CacheInterceptorUnitTest {

    private val now = Date(1234L)

    private fun <O : dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation> createToken(
            operation: O
    ): RequestToken<O, TestResponse> =
            RequestToken(
                    CacheInstruction(
                            operation,
                            HashedRequestMetadata(
                                    TestResponse::class.java,
                                    "http://test.com/test",
                                    null,
                                    INVALID_HASH,
                                    INVALID_HASH
                            )
                    ),
                    INSTRUCTION,
                    now
            )

    @Test
    fun `Cache operation delegates to CacheManager getCachedResponse`() = runTest {
        val mockCacheManager = mock<CacheManager<DejaVuError>>()
        val token = createToken(Cache(durationInSeconds = 3600))

        val expectedResponse = Response<TestResponse, Cache>(
                TestResponse(),
                ResponseToken(token.instruction as CacheInstruction<Cache, TestResponse>, FRESH, now),
                CallDuration(0, 0, 0)
        )

        whenever(mockCacheManager.getCachedResponse(any(), any()))
                .thenReturn(flowOf(expectedResponse))

        val interceptor = CacheInterceptor.Factory<DejaVuError>(mockCacheManager)
                .create(token)

        val upstream = flowOf(expectedResponse as dev.pthomain.android.dejavu.cache.metadata.response.ResultWrapper<TestResponse>)
        val results = interceptor.intercept(upstream).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Response<*, *>)
    }

    @Test
    fun `Clear operation delegates to CacheManager clearCache`() = runTest {
        val mockCacheManager = mock<CacheManager<DejaVuError>>()
        val token = createToken(Clear())

        val expectedResult = Result<TestResponse, Clear>(
                RequestToken(token.instruction as CacheInstruction<Clear, TestResponse>, DONE, now),
                CallDuration(0, 0, 0)
        )

        whenever(mockCacheManager.clearCache(any<RequestToken<Clear, TestResponse>>()))
                .thenReturn(flowOf(expectedResult))

        val interceptor = CacheInterceptor.Factory<DejaVuError>(mockCacheManager)
                .create(token)

        val upstream = flowOf(mock<dev.pthomain.android.dejavu.cache.metadata.response.ResultWrapper<TestResponse>>())
        val results = interceptor.intercept(upstream).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Result<*, *>)
        assertEquals(DONE, (results[0] as Result<*, *>).cacheToken.status)
    }

    @Test
    fun `Invalidate operation delegates to CacheManager invalidate`() = runTest {
        val mockCacheManager = mock<CacheManager<DejaVuError>>()
        val token = createToken(Invalidate)

        val expectedResult = Result<TestResponse, Invalidate>(
                RequestToken(token.instruction as CacheInstruction<Invalidate, TestResponse>, DONE, now),
                CallDuration(0, 0, 0)
        )

        whenever(mockCacheManager.invalidate(any<RequestToken<Invalidate, TestResponse>>()))
                .thenReturn(flowOf(expectedResult))

        val interceptor = CacheInterceptor.Factory<DejaVuError>(mockCacheManager)
                .create(token)

        val upstream = flowOf(mock<dev.pthomain.android.dejavu.cache.metadata.response.ResultWrapper<TestResponse>>())
        val results = interceptor.intercept(upstream).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Result<*, *>)
    }

    @Test
    fun `DoNotCache operation updates status to NOT_CACHED`() = runTest {
        val mockCacheManager = mock<CacheManager<DejaVuError>>()
        val token = createToken(DoNotCache)

        val upstreamResponse = Response<TestResponse, DoNotCache>(
                TestResponse(),
                ResponseToken(
                        CacheInstruction(DoNotCache, HashedRequestMetadata(
                                TestResponse::class.java, "http://test.com/test", null, INVALID_HASH, INVALID_HASH
                        )),
                        NETWORK,
                        now
                ),
                CallDuration(0, 0, 0)
        )

        val interceptor = CacheInterceptor.Factory<DejaVuError>(mockCacheManager)
                .create(token)

        @Suppress("UNCHECKED_CAST")
        val upstream = flowOf(upstreamResponse as dev.pthomain.android.dejavu.cache.metadata.response.ResultWrapper<TestResponse>)
        val results = interceptor.intercept(upstream).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Response<*, *>)
        assertEquals(NOT_CACHED, (results[0] as Response<*, *>).cacheToken.status)
    }
}
