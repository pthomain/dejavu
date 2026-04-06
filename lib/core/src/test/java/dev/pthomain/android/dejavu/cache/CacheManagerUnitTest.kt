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
import dev.pthomain.android.dejavu.cache.metadata.response.Empty
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
import dev.pthomain.android.dejavu.error.DejaVuError
import dev.pthomain.android.dejavu.error.DejaVuErrorFactory
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseFactory
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.utils.SilentLogger
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class CacheManagerUnitTest {

    private val now = Date(1234L)
    private val dateFactory: (Long?) -> Date = { if (it == null) now else Date(it) }

    private fun createCacheInstruction(operation: Cache = Cache(durationInSeconds = 3600)) =
            CacheInstruction<Cache, TestResponse>(
                    operation,
                    HashedRequestMetadata(
                            TestResponse::class.java,
                            "http://test.com/test",
                            null,
                            INVALID_HASH,
                            INVALID_HASH
                    )
            )

    private fun createManager(
            persistenceManager: PersistenceManager = mock()
    ): CacheManager<DejaVuError> {
        val errorFactory = DejaVuErrorFactory()
        val cacheMetadataManager = CacheMetadataManager<DejaVuError>(
                errorFactory, persistenceManager, dateFactory, { null }, SilentLogger
        )
        val emptyResponseFactory = EmptyResponseFactory<DejaVuError>(errorFactory, dateFactory)
        return CacheManager(persistenceManager, cacheMetadataManager, emptyResponseFactory, dateFactory, SilentLogger)
    }

    @Test
    fun `clearCache emits a Result with DONE status`() = runTest {
        val manager = createManager()
        val instruction = CacheInstruction<Clear, TestResponse>(
                Clear(),
                HashedRequestMetadata(TestResponse::class.java, "http://test.com/test", null, INVALID_HASH, INVALID_HASH)
        )
        val token = RequestToken(instruction, INSTRUCTION, now)

        val results = manager.clearCache(token).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Result<*, *>)
        assertEquals(DONE, (results[0] as Result<*, *>).cacheToken.status)
    }

    @Test
    fun `invalidate emits a Result with DONE status`() = runTest {
        val manager = createManager()
        val instruction = CacheInstruction<Invalidate, TestResponse>(
                Invalidate,
                HashedRequestMetadata(TestResponse::class.java, "http://test.com/test", null, INVALID_HASH, INVALID_HASH)
        )
        val token = RequestToken(instruction, INSTRUCTION, now)

        val results = manager.invalidate(token).toList()

        assertEquals(1, results.size)
        assertTrue(results[0] is Result<*, *>)
        assertEquals(DONE, (results[0] as Result<*, *>).cacheToken.status)
    }

    @Test
    fun `getCachedResponse returns empty when no cache and offline`() = runTest {
        val mockPersistence = mock<PersistenceManager>()
        whenever(mockPersistence.get(any<RequestToken<Cache, TestResponse>>())).thenReturn(null)

        val manager = createManager(mockPersistence)

        val operation = Cache(
                durationInSeconds = 3600,
                priority = dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.OFFLINE_FIRST
        )
        val instruction = createCacheInstruction(operation)
        val token = RequestToken(instruction, INSTRUCTION, now)

        val upstream = flowOf<DejaVuResult<TestResponse>>()

        val results = manager.getCachedResponse(upstream, token).toList()

        assertEquals(1, results.size)
        assertTrue("Expected Empty result when no cache available offline",
                results[0] is Empty<*, *, *>)
    }

    @Test
    fun `getCachedResponse fetches from network when no cache exists`() = runTest {
        val mockPersistence = mock<PersistenceManager>()
        whenever(mockPersistence.get(any<RequestToken<Cache, TestResponse>>())).thenReturn(null)

        val manager = createManager(mockPersistence)

        val operation = Cache(durationInSeconds = 3600)
        val instruction = createCacheInstruction(operation)
        val token = RequestToken(instruction, INSTRUCTION, now)

        val networkResponse = Response<TestResponse, Cache>(
                TestResponse(),
                ResponseToken(instruction, NETWORK, now),
                CallDuration(0, 10, 0)
        )

        val upstream = flowOf<DejaVuResult<TestResponse>>(networkResponse)

        val results = manager.getCachedResponse(upstream, token).toList()

        assertTrue("Should have at least one result from network", results.isNotEmpty())
    }
}
