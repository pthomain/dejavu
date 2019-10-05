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
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.token.CacheStatus.*
import dev.pthomain.android.dejavu.interceptors.internal.error.Glitch
import dev.pthomain.android.dejavu.response.CacheMetadata
import dev.pthomain.android.dejavu.response.ResponseWrapper
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import dev.pthomain.android.dejavu.test.instructionToken
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.test.operationSequence
import dev.pthomain.android.dejavu.test.trueFalseSequence
import org.junit.Test
import java.io.IOException
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


    //TODO finish
}