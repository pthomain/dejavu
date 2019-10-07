/*
 *
 *  Copyright (C) 2017 Pierre Thomain
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

package dev.pthomain.android.dejavu.interceptors.internal.cache

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Expiring.*
import dev.pthomain.android.dejavu.configuration.ErrorFactory
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.token.CacheStatus.*
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.internal.error.Glitch
import dev.pthomain.android.dejavu.response.ResponseWrapper
import dev.pthomain.android.dejavu.retrofit.annotations.CacheException
import dev.pthomain.android.dejavu.retrofit.annotations.CacheException.Type.SERIALISATION
import dev.pthomain.android.dejavu.test.*
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import org.junit.Test
import java.io.IOException
import java.io.NotSerializableException
import java.util.*

class CacheMetadataManagerUnitTest {

    private lateinit var mockErrorFactory: ErrorFactory<Glitch>
    private lateinit var mockPersistenceManager: PersistenceManager<Glitch>
    private lateinit var mockDateFactory: (Long?) -> Date

    private val now = 321L
    private val diskDuration = 5
    private val networkDuration = 20
    private val defaultDurationInMillis = 12345L
    private val operationDurationInMillis = 78988L
    private val previousCacheDate = Date(456L)
    private val previousExpiryDate = Date(567L)

    private lateinit var target: CacheMetadataManager<Glitch>

    private fun setUp() {
        mockErrorFactory = mock()
        mockPersistenceManager = mock()
        mockDateFactory = mock()

        target = CacheMetadataManager(
                mockErrorFactory,
                mockPersistenceManager,
                mockDateFactory,
                defaultDurationInMillis,
                mock()
        )
    }

    @Test
    fun testSetNetworkCallMetadata() {
        var iteration = 0
        operationSequence { operation ->
            if (operation is Expiring && operation !is Offline) {
                trueFalseSequence { hasOperationDuration ->
                    trueFalseSequence { hasCachedResponse ->
                        trueFalseSequence { networkCallFails ->
                            trueFalseSequence { encryptData ->
                                trueFalseSequence { compressData ->
                                    testSetNetworkCallMetadata(
                                            iteration++,
                                            operation,
                                            hasOperationDuration,
                                            hasCachedResponse,
                                            networkCallFails,
                                            encryptData,
                                            compressData
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun testSetNetworkCallMetadata(iteration: Int,
                                           operation: Expiring,
                                           hasOperationDuration: Boolean,
                                           hasCachedResponse: Boolean,
                                           networkCallFails: Boolean,
                                           encryptData: Boolean,
                                           compressData: Boolean) {
        setUp()
        val context = "iteration = $iteration,\n" +
                "operation = ${operation.type},\n" +
                "hasOperationDuration = $hasOperationDuration\n" +
                "hasCachedResponse = $hasCachedResponse\n" +
                "networkCallFails = $networkCallFails\n" +
                "encryptData = $encryptData\n" +
                "compressData = $compressData\n"

        val operationWithDuration = when (operation) {
            is Cache -> Cache(
                    freshOnly = operation.freshOnly,
                    filterFinal = operation.filterFinal,
                    mergeOnNextOnError = operation.mergeOnNextOnError,
                    durationInMillis = ifElse(hasOperationDuration, operationDurationInMillis, null)
            )

            is Refresh -> Refresh(
                    freshOnly = operation.freshOnly,
                    filterFinal = operation.filterFinal,
                    mergeOnNextOnError = operation.mergeOnNextOnError,
                    durationInMillis = ifElse(hasOperationDuration, operationDurationInMillis, null)
            )

            else -> throw IllegalStateException("Missing case for operation $operation")
        }

        val instructionToken = instructionToken(operationWithDuration)

        val networkGlitch = Glitch(IOException("Network"))

        val metadata = CacheMetadata(
                instructionToken,
                ifElse(networkCallFails, networkGlitch, null),
                CacheMetadata.Duration(diskDuration, networkDuration, 0)
        )

        val responseWrapper = ResponseWrapper(
                TestResponse::class.java,
                ifElse(networkCallFails, null, mock<TestResponse>()),
                metadata
        )

        val previousToken = instructionToken.copy(
                status = STALE,
                cacheDate = previousCacheDate,
                expiryDate = previousExpiryDate
        )

        val previousWrapper = ResponseWrapper<Glitch>(
                TestResponse::class.java,
                mock<TestResponse>(),
                CacheMetadata(previousToken)
        )

        val previousCachedResponse = ifElse(
                hasCachedResponse,
                previousWrapper,
                null
        )

        val expectedDuration = ifElse(
                hasOperationDuration,
                operationDurationInMillis,
                defaultDurationInMillis
        )

        whenever(mockDateFactory.invoke(isNull())).thenReturn(Date(now))
        whenever(mockDateFactory.invoke(eq(now + expectedDuration))).thenReturn(Date(now + expectedDuration))

        whenever(mockPersistenceManager.shouldEncryptOrCompress(
                if (hasCachedResponse) eq(previousCachedResponse) else isNull(),
                eq(operationWithDuration)
        )).thenReturn(Pair(encryptData, compressData))

        val actualWrapper = target.setNetworkCallMetadata(
                responseWrapper,
                operationWithDuration,
                previousCachedResponse,
                instructionToken,
                diskDuration
        )

        val expectedStatus = ifElse(
                networkCallFails,
                ifElse(
                        operation.freshOnly,
                        EMPTY,
                        ifElse(hasCachedResponse, COULD_NOT_REFRESH, EMPTY)
                ),
                ifElse(hasCachedResponse, REFRESHED, NETWORK)
        )

        with(actualWrapper.metadata.cacheToken) {

            assertEqualsWithContext(
                    instructionToken.instruction,
                    instruction,
                    "Cache token instruction didn't match",
                    context
            )

            assertEqualsWithContext(
                    expectedStatus,
                    status,
                    "Cache status didn't match",
                    context
            )

            assertEqualsWithContext(
                    compressData,
                    isCompressed,
                    "isCompressed didn't match",
                    context
            )

            assertEqualsWithContext(
                    encryptData,
                    isEncrypted,
                    "isEncrypted didn't match",
                    context
            )

            assertEqualsWithContext(
                    instructionToken.requestMetadata,
                    requestMetadata,
                    "requestMetadata didn't match",
                    context
            )

            assertEqualsWithContext(
                    Date(now),
                    fetchDate,
                    "fetchDate didn't match",
                    context
            )

            val expectedCacheDate = ifElse(
                    networkCallFails,
                    ifElse(hasCachedResponse, previousCacheDate, null),
                    Date(now)
            )

            assertEqualsWithContext(
                    ifElse(expectedStatus == EMPTY, null, expectedCacheDate),
                    cacheDate,
                    "cacheDate didn't match",
                    context
            )

            val expectedExpiryDate = ifElse(
                    networkCallFails,
                    ifElse(hasCachedResponse, previousExpiryDate, null),
                    Date(now + expectedDuration)
            )

            assertEqualsWithContext(
                    ifElse(expectedStatus == EMPTY, null, expectedExpiryDate),
                    expiryDate,
                    "expiryDate didn't match",
                    context
            )
        }

        assertEqualsWithContext(
                ifElse(expectedStatus == EMPTY, null, responseWrapper.response),
                actualWrapper.response,
                "Response didn't match",
                context
        )

        with(actualWrapper.metadata.callDuration) {

            assertEqualsWithContext(
                    diskDuration,
                    disk,
                    "Disk duration didn't match",
                    context
            )

            assertEqualsWithContext(
                    networkDuration - diskDuration,
                    network,
                    "Network duration didn't match",
                    context
            )


            assertEqualsWithContext(
                    networkDuration,
                    total,
                    "Total duration didn't match",
                    context
            )

        }
    }

    @Test
    fun testSetSerialisationFailedMetadata() {
        var iteration = 0
        operationSequence { operation ->
            if (operation is Expiring && operation !is Offline) {
                testSetSerialisationFailedMetadata(
                        iteration++,
                        operation
                )
            }
        }
    }

    private fun testSetSerialisationFailedMetadata(iteration: Int,
                                                   operation: Expiring) {
        setUp()
        val context = "iteration = $iteration,\n" +
                "operation = ${operation.type}"

        val instructionToken = instructionToken(operation).copy(
                cacheDate = Date(1234L),
                fetchDate = Date(1456L)
        )

        val cause = NotSerializableException()
        val mockGlitch = Glitch(cause)

        val metadata = CacheMetadata<Glitch>(
                instructionToken,
                null
        )

        val responseWrapper = ResponseWrapper(
                TestResponse::class.java,
                mock<TestResponse>(),
                metadata
        )

        val message = "Could not serialise ${TestResponse::class.java.simpleName}: this response will not be cached."

        val expectedException = CacheException(
                SERIALISATION,
                message,
                cause
        )

        whenever(mockErrorFactory.getError(eq(expectedException))).thenReturn(mockGlitch)

        val actualWrapper = target.setSerialisationFailedMetadata(
                responseWrapper,
                cause
        )

        assertEqualsWithContext(
                responseWrapper.response,
                actualWrapper.response,
                "Response didn't match",
                context
        )

        assertEqualsWithContext(
                responseWrapper.responseClass,
                actualWrapper.responseClass,
                "Response class didn't match",
                context
        )

        with(actualWrapper.metadata) {
            assertEqualsWithContext(
                    mockGlitch,
                    exception,
                    "Exception didn't match",
                    context
            )

            assertEqualsWithContext(
                    NOT_CACHED,
                    cacheToken.status,
                    "Cache status didn't match",
                    context
            )

            assertNullWithContext(
                    cacheToken.cacheDate,
                    "Cache date should be null",
                    context
            )

            assertNullWithContext(
                    cacheToken.expiryDate,
                    "Expiry date should be null",
                    context
            )
        }

    }

}