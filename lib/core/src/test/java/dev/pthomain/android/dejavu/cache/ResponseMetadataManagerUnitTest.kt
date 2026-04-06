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

import com.nhaarman.mockitokotlin2.mock
import dev.pthomain.android.dejavu.cache.metadata.response.CallDuration
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.*
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.CacheInstruction
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.INVALID_HASH
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.error.DejaVuError
import dev.pthomain.android.dejavu.error.DejaVuErrorFactory
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.utils.SilentLogger
import org.junit.Test
import java.util.*

/**
 * Tests for CacheMetadataManager, which replaced the old ResponseMetadataManager.
 */
class ResponseMetadataManagerUnitTest {

    private val now = Date(321L)
    private val diskDuration = 5
    private val networkDuration = 20

    private fun createTarget() = CacheMetadataManager<DejaVuError>(
            DejaVuErrorFactory(),
            mock<PersistenceManager>(),
            { if (it == null) now else Date(it) },
            { null },
            SilentLogger
    )

    private fun createInstruction(operation: Cache = Cache(durationInSeconds = 3600)) =
            CacheInstruction<Cache, TestResponse>(
                    operation,
                    HashedRequestMetadata(
                            TestResponse::class.java,
                            "http://test.com/testResponse",
                            null,
                            INVALID_HASH,
                            INVALID_HASH
                    )
            )

    @Test
    fun testSetNetworkCallMetadataWithoutCachedResponse() {
        val target = createTarget()
        val operation = Cache(durationInSeconds = 3600)
        val instruction = createInstruction(operation)
        val instructionToken = RequestToken(instruction, INSTRUCTION, now)

        val responseWrapper = Response(
                TestResponse(),
                ResponseToken(instruction, NETWORK, now),
                CallDuration(0, networkDuration, 0)
        )

        val result = target.setNetworkCallMetadata(
                responseWrapper,
                operation,
                null,
                instructionToken,
                diskDuration
        )

        assertEqualsWithContext(
                NETWORK,
                result.cacheToken.status,
                "Status should be NETWORK when no previous cached response exists"
        )

        assertEqualsWithContext(
                diskDuration,
                result.callDuration.disk,
                "Disk duration should match"
        )
    }

    @Test
    fun testSetNetworkCallMetadataWithCachedResponse() {
        val target = createTarget()
        val operation = Cache(durationInSeconds = 3600)
        val instruction = createInstruction(operation)
        val instructionToken = RequestToken(instruction, INSTRUCTION, now)

        val responseWrapper = Response(
                TestResponse(),
                ResponseToken(instruction, NETWORK, now),
                CallDuration(0, networkDuration, 0)
        )

        val previousCachedResponse = Response(
                TestResponse(),
                ResponseToken(instruction, STALE, now, Date(0)),
                CallDuration(0, 0, 0)
        )

        val result = target.setNetworkCallMetadata(
                responseWrapper,
                operation,
                previousCachedResponse,
                instructionToken,
                diskDuration
        )

        assertEqualsWithContext(
                REFRESHED,
                result.cacheToken.status,
                "Status should be REFRESHED when previous cached response exists"
        )
    }

    @Test
    fun testSetSerialisationFailedMetadata() {
        val target = createTarget()
        val operation = Cache(durationInSeconds = 3600)
        val instruction = createInstruction(operation)

        val responseWrapper = Response(
                TestResponse(),
                ResponseToken(instruction, NETWORK, now),
                CallDuration(0, 0, 0)
        )

        val cause = java.io.NotSerializableException()

        val result = target.setSerialisationFailedMetadata(
                responseWrapper,
                cause
        )

        assertEqualsWithContext(
                NOT_CACHED,
                result.cacheToken.status,
                "Status should be NOT_CACHED after serialisation failure"
        )

        assertEqualsWithContext(
                responseWrapper.response,
                result.response,
                "Response data should be preserved"
        )
    }
}
