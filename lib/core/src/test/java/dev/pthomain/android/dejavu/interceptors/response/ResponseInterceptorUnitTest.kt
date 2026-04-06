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

package dev.pthomain.android.dejavu.interceptors.response

import dev.pthomain.android.dejavu.cache.metadata.response.CallDuration
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.response.Empty
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.*
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.CacheInstruction
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.INVALID_HASH
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.error.DejaVuError
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.utils.SilentLogger
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.*

class ResponseInterceptorUnitTest {

    private val now = Date(1234L)
    private val dateFactory: (Long?) -> Date = { if (it == null) now else Date(it) }

    private fun createInstruction() = CacheInstruction<Cache, TestResponse>(
            Cache(durationInSeconds = 3600),
            HashedRequestMetadata(
                    TestResponse::class.java,
                    "http://test.com/test",
                    null,
                    INVALID_HASH,
                    INVALID_HASH
            )
    )

    @Test
    fun `asResult=true returns DejaVuResult directly`() = runTest {
        val instruction = createInstruction()
        val response = Response<TestResponse, Cache>(
                TestResponse(),
                ResponseToken(instruction, FRESH, now),
                CallDuration(0, 10, 10)
        )

        val interceptor = ResponseInterceptor.Factory<DejaVuError>(SilentLogger, dateFactory)
                .create<TestResponse>(asResult = true)

        val results = interceptor.intercept(flowOf(response)).toList()

        assertEquals(1, results.size)
        assertTrue("asResult=true should return DejaVuResult", results[0] is DejaVuResult<*>)
    }

    @Test
    fun `asResult=false returns unwrapped response`() = runTest {
        val instruction = createInstruction()
        val testResponse = TestResponse()
        val response = Response<TestResponse, Cache>(
                testResponse,
                ResponseToken(instruction, FRESH, now),
                CallDuration(0, 10, 10)
        )

        val interceptor = ResponseInterceptor.Factory<DejaVuError>(SilentLogger, dateFactory)
                .create<TestResponse>(asResult = false)

        val results = interceptor.intercept(flowOf(response)).toList()

        assertEquals(1, results.size)
        assertEquals(testResponse, results[0])
    }

    @Test
    fun `asResult=false with Empty result throws exception`() = runTest {
        val instruction = createInstruction()
        val error = DejaVuError(IOException("test"))
        val empty = Empty<TestResponse, Cache, DejaVuError>(
                error,
                RequestToken(instruction, EMPTY, now),
                CallDuration(0, 0, 0)
        )

        val interceptor = ResponseInterceptor.Factory<DejaVuError>(SilentLogger, dateFactory)
                .create<TestResponse>(asResult = false)

        try {
            interceptor.intercept(flowOf(empty)).toList()
            assertTrue("Should have thrown", false)
        } catch (e: DejaVuError) {
            assertEquals(error, e)
        }
    }
}
