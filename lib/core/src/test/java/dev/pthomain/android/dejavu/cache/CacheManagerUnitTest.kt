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

import com.google.common.net.HttpHeaders.REFRESH
import com.nhaarman.mockitokotlin2.*
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.*
import dev.pthomain.android.dejavu.cache.metadata.token.InstructionToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.error.glitch.Glitch
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseFactory
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.dejavu.test.*
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import java.io.IOException
import java.io.NotSerializableException
import java.util.*

class CacheManagerUnitTest {

    private lateinit var mockErrorFactory: ErrorFactory<Glitch>
    private lateinit var mockCacheMetadataManager: CacheMetadataManager<Glitch>
    private lateinit var mockPersistenceManager: PersistenceManager<Glitch>
    private lateinit var mockEmptyResponseFactory: EmptyResponseFactory<Glitch>
    private lateinit var mockDateFactory: (Long?) -> Date
    private lateinit var mockNetworkGlitch: Glitch
    private lateinit var mockNetworkResponseWrapper: ResponseWrapper<*, *, Glitch>
    private var mockCachedResponseWrapper: ResponseWrapper<*, *, Glitch>? = null
    private lateinit var mockSerialisationGlitch: Glitch
    private lateinit var mockSerialisationErrorResponseWrapper: ResponseWrapper<*, *, Glitch>
    private lateinit var mockEmptyResponseWrapper: ResponseWrapper<*, *, Glitch>
    private lateinit var mockPersistedResponseWrapper: ResponseWrapper<*, *, Glitch>
    private lateinit var mockUpdatedMetatadataNetworkResponseWrapper: ResponseWrapper<*, *, Glitch>

    private val now = Date(1000L)
    private val start = 100L
    private val illegalStateException = IllegalStateException("Error")

    private lateinit var target: CacheManager<Glitch>

    private fun setUp() {
        mockErrorFactory = mock()
        mockPersistenceManager = mock()
        mockEmptyResponseFactory = mock()
        mockDateFactory = mock()
        mockNetworkGlitch = Glitch(IOException("Network error"))
        mockSerialisationGlitch = Glitch(NotSerializableException("Serialisation"))
        mockCacheMetadataManager = mock()
        mockEmptyResponseWrapper = mock()
        mockPersistedResponseWrapper = mock()

        target = CacheManager(
                mockPersistenceManager,
                mockCacheMetadataManager,
                mockEmptyResponseFactory,
                mockDateFactory,
                mock()
        )
    }

    @Test
    fun testClearCache() {
        var iteration = 0
        trueFalseSequence { hasTypeToClear ->
            trueFalseSequence { clearStaleEntriesOnly ->
                testClearCache(
                        iteration++,
                        if (hasTypeToClear) TestResponse::class.java else null,
                        clearStaleEntriesOnly
                )
            }
        }
    }

    private fun testClearCache(iteration: Int,
                               typeToClear: Class<*>?,
                               clearStaleEntriesOnly: Boolean) {
        setUp()
        val context = "iteration = $iteration\n" +
                "typeToClear = $typeToClear,\n" +
                "clearStaleEntriesOnly = $clearStaleEntriesOnly"

        val instructionToken = instructionToken()
        val mockResponseWrapper = mock<ResponseWrapper<*, *, Glitch>>()

        whenever(mockEmptyResponseFactory.create(
                eq(instructionToken)
        )).thenReturn(mockResponseWrapper)

        val actualResponseWrapper = target.clearCache(
                instructionToken
        ).blockingFirst()

        verifyWithContext(mockPersistenceManager, context).clearCache(eq(instructionToken))

        assertEqualsWithContext(
                mockResponseWrapper,
                actualResponseWrapper,
                "Returned response wrapper didn't match",
                context
        )
    }

    @Test
    fun testInvalidate() {
        setUp()
        val instructionToken = instructionToken()
        val mockResponseWrapper = mock<ResponseWrapper<*, *, Glitch>>()

        whenever(mockEmptyResponseFactory.create(
                eq(instructionToken)
        )).thenReturn(mockResponseWrapper)

        val actualResponseWrapper = target.invalidate(instructionToken).blockingFirst()

        verify(mockPersistenceManager).invalidate(eq(instructionToken))

        assertEqualsWithContext(
                mockResponseWrapper,
                actualResponseWrapper,
                "Returned response wrapper didn't match"
        )
    }

    @Test
    fun testGetCachedResponse() {
        var iteration = 0
        operationSequence { operation ->
            if (operation is Cache) {
                trueFalseSequence { hasCachedResponse ->
                    trueFalseSequence { networkCallFails ->
                        trueFalseSequence { serialisationFails ->
                            trueFalseSequence { isResponseStale ->
                                testGetCachedResponse(
                                        iteration++,
                                        operation,
                                        hasCachedResponse,
                                        networkCallFails,
                                        serialisationFails,
                                        hasCachedResponse && isResponseStale
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun testGetCachedResponse(iteration: Int,
                                      operation: Cache,
                                      hasCachedResponse: Boolean,
                                      networkCallFails: Boolean,
                                      serialisationFails: Boolean,
                                      isResponseStale: Boolean) {
        setUp()
        val context = "iteration = $iteration,\n" +
                "operation = ${operation.type},\n" +
                "operation.priority = ${operation.priority},\n" +
                "hasCachedResponse = $hasCachedResponse\n" +
                "isResponseStale = $isResponseStale\n" +
                "networkCallFails = $networkCallFails\n" +
                "serialisationFails = $serialisationFails\n"

        val instructionToken = instructionToken(operation)
        val isResponseStaleOverall = isResponseStale || operation.priority.network == REFRESH

        val mockPreviousCacheMetadata = ResponseMetadata(
                instructionToken.copy(status = ifElse(isResponseStaleOverall, STALE, FRESH)),
                Glitch::class.java
        )

        val mockNetworkSuccessResponseWrapper = defaultResponseWrapper(
                ResponseMetadata(
                        instructionToken.copy(status = NETWORK),
                        Glitch::class.java
                ),
                mock()
        )

        val mockNetworkFailureResponseWrapper = mockNetworkSuccessResponseWrapper.copy(
                metadata = mockNetworkSuccessResponseWrapper.metadata.copy(exception = mockNetworkGlitch),
                response = mock<TestResponse>()
        )

        mockNetworkResponseWrapper = ifElse(
                networkCallFails,
                mockNetworkFailureResponseWrapper,
                mockNetworkSuccessResponseWrapper
        )

        mockCachedResponseWrapper = ifElse(
                hasCachedResponse,
                defaultResponseWrapper(
                        metadata = mockPreviousCacheMetadata,
                        response = mock()
                ),
                null
        )

        whenever(mockPersistenceManager.getCachedResponse(eq(instructionToken)))
                .thenReturn(if (hasCachedResponse) mockCachedResponseWrapper else null)

        whenever(mockDateFactory.invoke(isNull())).thenReturn(now)

        if (operation.priority.network == OFFLINE) {
            if (!hasCachedResponse) {
                whenever(mockEmptyResponseFactory.create(
                        eq(instructionToken)
                )).thenReturn(mockEmptyResponseWrapper)
            }
        } else if (!hasCachedResponse || isResponseStaleOverall) {
            prepareFetchAndCache(
                    operation,
                    instructionToken,
                    hasCachedResponse,
                    serialisationFails
            )
        }

        val testObserver = TestObserver<ResponseWrapper<*, *, Glitch>>()

        target.getCachedResponse(
                Observable.just(mockNetworkResponseWrapper),
                instructionToken,
                start
        ).subscribe(testObserver)

        if (operation.priority.network == OFFLINE) {
            if (hasCachedResponse) {
                assertEqualsWithContext(
                        mockCachedResponseWrapper,
                        getSingleActualResponse(context, testObserver),
                        "The response didn't match",
                        context
                )
            } else {
                assertEqualsWithContext(
                        mockEmptyResponseWrapper,
                        getSingleActualResponse(context, testObserver),
                        "The response should be empty when no cached response exists and the operation is OFFLINE",
                        context
                )
            }
        } else {
            verifyFetchAndCache(
                    testObserver,
                    context,
                    operation,
                    hasCachedResponse,
                    isResponseStaleOverall,
                    networkCallFails,
                    serialisationFails
            )
        }
    }

    private fun getSingleActualResponse(context: String,
                                        testObserver: TestObserver<ResponseWrapper<*, *, Glitch>>): ResponseWrapper<*, *, Glitch> {
        assertTrueWithContext(
                testObserver.errorCount() == 0,
                "Expected no error",
                context
        )
        assertTrueWithContext(
                testObserver.valueCount() == 1,
                "Expected exactly one response",
                context
        )

        return testObserver.values().first()
    }

    private fun getResponsePair(context: String,
                                testObserver: TestObserver<ResponseWrapper<*, *, Glitch>>,
                                serialisationFails: Boolean,
                                networkCallFails: Boolean): Pair<ResponseWrapper<*, *, Glitch>, ResponseWrapper<*, *, Glitch>?> {
        assertEqualsWithContext(
                null,
                testObserver.errors().firstOrNull(),
                "Expected no error",
                context
        )

        assertEqualsWithContext(
                2,
                testObserver.valueCount(),
                "Expected exactly two responses",
                context
        )

        val secondResponse = testObserver.values()[1]

        if (!networkCallFails && serialisationFails) {
            assertEqualsWithContext(
                    mockSerialisationErrorResponseWrapper,
                    secondResponse,
                    "Expected a serialisation error",
                    context
            )
        }

        return Pair(testObserver.values()[0], secondResponse)
    }

    private fun prepareFetchAndCache(operation: Cache,
                                     instructionToken: InstructionToken,
                                     hasCachedResponse: Boolean,
                                     serialisationFails: Boolean) {
        mockUpdatedMetatadataNetworkResponseWrapper = mockNetworkResponseWrapper.copy(
                metadata = mockNetworkResponseWrapper.metadata.copy(cacheToken =
                mockNetworkResponseWrapper.metadata.cacheToken.copy(
                        fetchDate = Date(123L),
                        cacheDate = Date(345L),
                        expiryDate = Date(567L)
                ))
        )

        whenever(mockCacheMetadataManager.setNetworkCallMetadata(
                eq(mockNetworkResponseWrapper),
                eq(operation),
                eq(mockCachedResponseWrapper),
                eq(instructionToken),
                anyInt()
        )).thenReturn(mockUpdatedMetatadataNetworkResponseWrapper)

        if (serialisationFails) {
            whenever(mockPersistenceManager.cache(
                    eq(mockUpdatedMetatadataNetworkResponseWrapper),
                    if (hasCachedResponse) eq(mockCachedResponseWrapper) else isNull()
            )).thenThrow(illegalStateException)

            mockSerialisationErrorResponseWrapper = mockUpdatedMetatadataNetworkResponseWrapper.copy(
                    metadata = mockUpdatedMetatadataNetworkResponseWrapper.metadata.copy(
                            exception = mockSerialisationGlitch
                    )
            )

            whenever(mockCacheMetadataManager.setSerialisationFailedMetadata(
                    eq(mockUpdatedMetatadataNetworkResponseWrapper),
                    eq(illegalStateException)
            )).thenReturn(mockSerialisationErrorResponseWrapper)
        }
    }

    private fun verifyFetchAndCache(testObserver: TestObserver<ResponseWrapper<*, *, Glitch>>,
                                    context: String,
                                    operation: Cache,
                                    hasCachedResponse: Boolean,
                                    isResponseStale: Boolean,
                                    networkCallFails: Boolean,
                                    serialisationFails: Boolean) {

        val hasSingleResponse = (isResponseStale && operation.priority.freshness == FRESH_ONLY)
                || !hasCachedResponse
                || !isResponseStale

        assertNullWithContext(
                testObserver.errors().firstOrNull(),
                "Exception should be null",
                context
        )

        val (actualResponseWrapper, expectedSecondResponseWrapper) = if (hasSingleResponse) {
            val actualResponseWrapper = getSingleActualResponse(context, testObserver)

            val hasFreshCachedResponse = hasCachedResponse && !isResponseStale

            if (!hasFreshCachedResponse && networkCallFails) {
                assertEqualsWithContext(
                        mockNetworkGlitch,
                        actualResponseWrapper.metadata.exception,
                        "The updated network metadata should have a network exception",
                        context
                )
            }

            actualResponseWrapper to ifElse(
                    hasFreshCachedResponse,
                    mockCachedResponseWrapper,
                    ifElse(
                            !networkCallFails && serialisationFails,
                            mockSerialisationErrorResponseWrapper,
                            mockUpdatedMetatadataNetworkResponseWrapper
                    )
            )
        } else {
            val (firstResponse, secondResponse) = getResponsePair(
                    context,
                    testObserver,
                    serialisationFails,
                    networkCallFails
            )

            assertEqualsWithContext(
                    mockCachedResponseWrapper,
                    firstResponse,
                    "The first returned response should be the cached one",
                    context
            )

            if (!networkCallFails) {
                verifyWithContext(mockPersistenceManager, context)
                        .cache(
                                eq(mockUpdatedMetatadataNetworkResponseWrapper),
                                if (hasCachedResponse) eq(mockCachedResponseWrapper) else isNull()
                        )
            }

            secondResponse to ifElse(
                    networkCallFails,
                    mockUpdatedMetatadataNetworkResponseWrapper,
                    ifElse(
                            serialisationFails,
                            mockSerialisationErrorResponseWrapper,
                            mockUpdatedMetatadataNetworkResponseWrapper
                    )
            )
        }

        assertEqualsWithContext(
                expectedSecondResponseWrapper,
                actualResponseWrapper,
                "Returned second response wrapper didn't match",
                context
        )
    }
}