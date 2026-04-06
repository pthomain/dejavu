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

import dev.pthomain.android.dejavu.cache.metadata.response.Empty
import dev.pthomain.android.dejavu.cache.metadata.response.Result
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.DONE
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.INSTRUCTION
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.CacheInstruction
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.INVALID_HASH
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.error.DejaVuError
import dev.pthomain.android.dejavu.error.DejaVuErrorFactory
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class EmptyResponseFactoryUnitTest {

    private val now = Date(1234L)
    private val dateFactory: (Long?) -> Date = { if (it == null) now else Date(it) }
    private val errorFactory = DejaVuErrorFactory()
    private val target = EmptyResponseFactory<DejaVuError>(errorFactory, dateFactory)

    @Test
    fun `createEmptyResponse returns Empty with error for Cache operation`() {
        val instruction = CacheInstruction<Cache, TestResponse>(
                Cache(durationInSeconds = 3600),
                HashedRequestMetadata(TestResponse::class.java, "http://test.com", null, INVALID_HASH, INVALID_HASH)
        )
        val token = RequestToken(instruction, INSTRUCTION, now)

        val result = target.createEmptyResponse(token)

        assertTrue("Should be an Empty result", result is Empty<*, *, *>)
        assertTrue("Exception should be a DejaVuError",
                (result as Empty<*, *, *>).exception is DejaVuError)
    }

    @Test
    fun `createDoneResponse returns Result with DONE status for Clear operation`() {
        val instruction = CacheInstruction<Clear, TestResponse>(
                Clear(),
                HashedRequestMetadata(TestResponse::class.java, "http://test.com", null, INVALID_HASH, INVALID_HASH)
        )
        val token = RequestToken(instruction, INSTRUCTION, now)

        val result = target.createDoneResponse(token)

        assertTrue("Should be a Result", result is Result<*, *>)
        assertEquals(DONE, (result as Result<*, *>).cacheToken.status)
    }

    @Test
    fun `createEmptyResponseFlow emits single result for Cache operation`() = runTest {
        val instruction = CacheInstruction<Cache, TestResponse>(
                Cache(durationInSeconds = 3600),
                HashedRequestMetadata(TestResponse::class.java, "http://test.com", null, INVALID_HASH, INVALID_HASH)
        )
        val token = RequestToken(instruction, INSTRUCTION, now)

        val results = target.createEmptyResponseFlow(token).toList()

        assertEquals(1, results.size)
        assertTrue("Should emit an Empty result", results[0] is Empty<*, *, *>)
    }

    @Test
    fun `createEmptyResponseFlow emits DONE result for Clear operation`() = runTest {
        val instruction = CacheInstruction<Clear, TestResponse>(
                Clear(),
                HashedRequestMetadata(TestResponse::class.java, "http://test.com", null, INVALID_HASH, INVALID_HASH)
        )
        val token = RequestToken(instruction, INSTRUCTION, now)

        val results = target.createEmptyResponseFlow(token).toList()

        assertEquals(1, results.size)
        assertTrue("Should emit a Result", results[0] is Result<*, *>)
        assertEquals(DONE, (results[0] as Result<*, *>).cacheToken.status)
    }
}
