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

package dev.pthomain.android.dejavu.persistence

import com.nhaarman.mockitokotlin2.*
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.cache.metadata.response.CallDuration
import dev.pthomain.android.dejavu.configuration.error.glitch.Glitch
import dev.pthomain.android.dejavu.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.shared.token.CacheStatus.FRESH
import dev.pthomain.android.dejavu.shared.token.CacheStatus.STALE
import dev.pthomain.android.dejavu.shared.token.InstructionToken
import dev.pthomain.android.dejavu.shared.token.instruction.CacheInstruction
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Invalidate
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Type.INVALIDATE
import dev.pthomain.android.dejavu.shared.utils.Utils.swapLambdaWhen
import dev.pthomain.android.dejavu.test.*
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import org.junit.Test
import java.util.*

internal abstract class BasePersistenceManagerUnitTest<T : dev.pthomain.android.dejavu.persistence.PersistenceManager<Glitch>> {

    protected lateinit var mockSerialisationManager: dev.pthomain.android.dejavu.serialisation.SerialisationManager<Glitch>
    protected lateinit var mockDateFactory: (Long?) -> Date
    protected lateinit var mockCacheToken: InstructionToken
    protected lateinit var mockResponseWrapper: ResponseWrapper<*, *, Glitch>
    protected lateinit var mockMetadata: ResponseMetadata<Glitch>

    protected val mockHash = "mockHash"
    protected val mockBlob = byteArrayOf(1, 2, 3, 4, 5, 6, 8, 9)

    protected val currentDateTime = 10000L
    protected val mockFetchDateTime = 1000L
    protected val mockCacheDateTime = 100L
    protected val mockExpiryDateTime = 10L
    protected val mockCurrentDate = Date(currentDateTime)
    protected val mockFetchDate = Date(mockFetchDateTime)
    protected val mockCacheDate = Date(mockCacheDateTime)
    protected val mockExpiryDate = Date(mockExpiryDateTime)

    protected fun setUpConfiguration(cacheInstruction: CacheInstruction?): DejaVu.Configuration<Glitch> {
        mockSerialisationManager = mock()
        mockDateFactory = mock()
        mockCacheToken = mock()
        mockResponseWrapper = mock()
        mockMetadata = mock()

        whenever(mockDateFactory.invoke(isNull())).thenReturn(mockCurrentDate)
        whenever(mockDateFactory.invoke(eq(mockCacheDateTime))).thenReturn(mockCacheDate)
        whenever(mockDateFactory.invoke(eq(mockExpiryDateTime))).thenReturn(mockExpiryDate)

        whenever(mockCacheToken.fetchDate).thenReturn(mockFetchDate)
        whenever(mockCacheToken.cacheDate).thenReturn(mockCacheDate)
        whenever(mockCacheToken.expiryDate).thenReturn(mockExpiryDate)

        whenever(mockResponseWrapper.metadata).thenReturn(mockMetadata)
        whenever(mockMetadata.cacheToken).thenReturn(mockCacheToken)

        if (cacheInstruction != null) {
            whenever(mockCacheToken.instruction).thenReturn(cacheInstruction)
        }

        val mockConfiguration = mock<DejaVu.Configuration<Glitch>>()
        whenever(mockConfiguration.logger).thenReturn(mock())

        return mockConfiguration
    }

    protected abstract fun setUp(instructionToken: InstructionToken): T

    @Test
    fun testClearCache() {
        var index = 0
        trueFalseSequence { useTypeToClear ->
            trueFalseSequence { useRequestParameters ->
                trueFalseSequence { clearStaleEntriesOnly ->
                    testClearCache(
                            index++,
                            useTypeToClear,
                            useRequestParameters,
                            clearStaleEntriesOnly
                    )
                }
            }
        }
    }

    private fun testClearCache(index: Int,
                               useTypeToClear: Boolean,
                               useRequestParameters: Boolean,
                               clearStaleEntriesOnly: Boolean) {
        val context = "index = $index," +
                "\nuseTypeToClear = $useTypeToClear" +
                "\nuseRequestParameters = $useRequestParameters" +
                "\nclearStaleEntriesOnly = $clearStaleEntriesOnly"

        val operation = Operation.Clear(
                useRequestParameters,
                clearStaleEntriesOnly
        )

        val targetClass = ifElse(
                useTypeToClear,
                TestResponse::class.java,
                Any::class.java
        )

        val mockClassHash = "mockHash"

        val instructionToken = instructionToken(operation).swapLambdaWhen(useTypeToClear) {
            it!!.copy(
                    instruction = it.instruction.copy(
                            requestMetadata = it.instruction.requestMetadata.copy(
                                    responseClass = targetClass
                            )))
        }!!

        val target = setUp(instructionToken)

        prepareClearCache(
                context,
                instructionToken
        )

        target.clearCache(instructionToken)

        verifyClearCache(
                context,
                useTypeToClear,
                clearStaleEntriesOnly,
                mockClassHash
        )
    }

    protected open fun prepareClearCache(context: String,
                                         instructionToken: InstructionToken) = Unit

    protected abstract fun verifyClearCache(context: String,
                                            useTypeToClear: Boolean,
                                            clearStaleEntriesOnly: Boolean,
                                            mockClassHash: String)

    @Test
    fun testCache() {
        var iteration = 0
        operationSequence { operation ->
            if (operation is Cache) {
                trueFalseSequence { hasPreviousResponse ->
                    trueFalseSequence { isSerialisationSuccess ->
                        testCache(
                                iteration++,
                                operation,
                                hasPreviousResponse,
                                isSerialisationSuccess
                        )
                    }
                }
            }
        }
    }

    private fun testCache(iteration: Int,
                          operation: Cache,
                          hasPreviousResponse: Boolean,
                          isSerialisationSuccess: Boolean) {
        val context = "iteration = $iteration,\n" +
                "operation = $operation,\n" +
                "hasPreviousResponse = $hasPreviousResponse\n" +
                "isSerialisationSuccess = $isSerialisationSuccess"

        val instructionToken = instructionTokenWithHash(operation)

        val target = setUp(instructionToken)

        prepareCache(
                iteration,
                operation,
                hasPreviousResponse,
                isSerialisationSuccess
        )

        val mockPreviousResponse = if (hasPreviousResponse) mock<ResponseWrapper<*, *, Glitch>>() else null

        if (mockPreviousResponse != null) {
            val previousMetadata = ResponseMetadata(
                    instructionToken(),
                    Glitch::class.java,
                    null,
                    CallDuration(0, 0, 0)
            )
            whenever(mockPreviousResponse.metadata).thenReturn(previousMetadata)
        }

        val encryptData = mockPreviousResponse?.metadata?.cacheToken?.isEncrypted
                ?: operation.encrypt

        val compressData = mockPreviousResponse?.metadata?.cacheToken?.isCompressed
                ?: operation.compress

        val metadata = SerialisationDecorationMetadata(compressData, encryptData)

        whenever(mockSerialisationManager.serialise(
                eq(mockResponseWrapper),
                eq(metadata)
        )).thenReturn(if (isSerialisationSuccess) mockBlob else null)

        target.cache(
                mockResponseWrapper,
                mockPreviousResponse
        )

        verifyCache(
                context,
                iteration,
                instructionToken,
                operation,
                encryptData,
                compressData,
                hasPreviousResponse,
                isSerialisationSuccess
        )
    }

    protected open fun prepareCache(iteration: Int,
                                    operation: Cache,
                                    hasPreviousResponse: Boolean,
                                    isSerialisationSuccess: Boolean) = Unit

    protected abstract fun verifyCache(context: String,
                                       iteration: Int,
                                       instructionToken: InstructionToken,
                                       operation: Cache,
                                       encryptData: Boolean,
                                       compressData: Boolean,
                                       hasPreviousResponse: Boolean,
                                       isSerialisationSuccess: Boolean)

    @Test
    fun testInvalidate() {
        operationSequence { operation ->
            testInvalidate(operation)
        }
    }

    private fun testInvalidate(operation: Operation) {
        prepareAllInvalidation(operation) { context, instructionToken, target ->

            prepareInvalidate(
                    context,
                    operation,
                    instructionToken
            )

            target.invalidate(instructionToken)

            verifyWithContext(
                    target,
                    context
            ).invalidateIfNeeded(eq(instructionToken.copy(
                    instruction = instructionToken.instruction.copy(operation = Invalidate())
            )))
        }
    }

    private fun prepareAllInvalidation(operation: Operation,
                                       andThen: (String, InstructionToken, T) -> Unit) {
        val context = "operation = $operation"
        val instructionToken = instructionTokenWithHash(operation)

        val target = spy(setUp(instructionToken))

        andThen(context, instructionToken, target)
    }

    protected abstract fun prepareInvalidate(context: String,
                                             operation: Operation,
                                             instructionToken: InstructionToken)

    @Test
    fun testCheckInvalidation() {
        operationSequence { operation ->
            testCheckInvalidation(operation)
        }
    }

    private fun testCheckInvalidation(operation: Operation) {
        prepareAllInvalidation(operation) { context, instructionToken, target ->

            prepareCheckInvalidation(
                    context,
                    operation,
                    instructionToken
            )

            target.invalidateIfNeeded(instructionToken)

            verifyCheckInvalidation(
                    context,
                    operation,
                    instructionToken
            )
        }
    }

    protected open fun prepareCheckInvalidation(context: String,
                                                operation: Operation,
                                                instructionToken: InstructionToken) = Unit

    protected abstract fun verifyCheckInvalidation(context: String,
                                                   operation: Operation,
                                                   instructionToken: InstructionToken)

    @Test
    fun testGetCachedResponse() {
        var iteration = 0
        operationSequence { operation ->
            if (operation is Cache) {
                trueFalseSequence { hasResults ->
                    trueFalseSequence { isStale ->
                        testGetCachedResponse(
                                iteration++,
                                operation,
                                hasResults,
                                isStale
                        )
                    }
                }
            }
        }
    }

    private fun instructionTokenWithHash(operation: Operation) =
            with(instructionToken(operation)) {
                copy(
                        instruction = instruction.copy(
                                requestMetadata = instruction.requestMetadata.copy(
                                        responseClass = instruction.requestMetadata.responseClass
                                )
                        )
                )
            }

    private fun testGetCachedResponse(iteration: Int,
                                      operation: Cache,
                                      hasResponse: Boolean,
                                      isStale: Boolean) {
        val context = "iteration = $iteration,\n" +
                "operation = $operation,\n" +
                "hasResponse = $hasResponse,\n" +
                "isStale = $isStale"

        val instructionToken = instructionTokenWithHash(operation)

        val isCompressed = 1
        val isEncrypted = 1

        val target = spy(setUp(instructionToken))

        val isDataStale = isStale || operation.priority.network == REFRESH || operation.type == INVALIDATE
        val cacheDateTimeStamp = 98765L

        val expiryDateTime = ifElse(
                isDataStale,
                currentDateTime - 1L,
                currentDateTime + 1L
        )

        val mockExpiryDate = Date(ifElse(
                isDataStale,
                currentDateTime - 1L,
                currentDateTime + 1L
        ))

        whenever(mockDateFactory.invoke(eq(cacheDateTimeStamp))).thenReturn(mockCacheDate)
        whenever(mockDateFactory.invoke(eq(expiryDateTime))).thenReturn(mockExpiryDate)

        val mockResponseWrapper = ResponseWrapper<*, *, Glitch>(
                TestResponse::class.java,
                null,
                mock()
        )

        val metadata = SerialisationDecorationMetadata(
                isCompressed == 1,
                isEncrypted == 1
        )

        whenever(mockSerialisationManager.deserialise(
                eq(instructionToken),
                eq(mockBlob),
                eq(metadata)
        )).thenReturn(ifElse(hasResponse, mockResponseWrapper, null))

        prepareGetCachedResponse(
                context,
                operation,
                instructionToken,
                hasResponse,
                isDataStale,
                isCompressed,
                isEncrypted,
                cacheDateTimeStamp,
                expiryDateTime
        )

        val cachedResponse = target.getCachedResponse(instructionToken)

        verifyWithContext(target, context)
                .invalidateIfNeeded(eq(instructionToken))

        verifyGetCachedResponse(
                context,
                operation,
                instructionToken,
                hasResponse,
                isDataStale,
                cachedResponse
        )

        if (hasResponse) {
            assertNotNullWithContext(
                    cachedResponse,
                    "Cachec response should not be null",
                    context
            )

            val actualMetadata = cachedResponse!!.metadata

            assertEqualsWithContext(
                    CallDuration(0, 0, 0),
                    actualMetadata.callDuration,
                    "Metadata call duration didn't match",
                    context
            )

            assertEqualsWithContext(
                    ifElse(isDataStale, STALE, FRESH),
                    actualMetadata.cacheToken.status,
                    "Cache status should be ${ifElse(isDataStale, "STALE", "FRESH")}",
                    context
            )
        } else {
            assertNullWithContext(
                    cachedResponse,
                    "Returned response should be null",
                    context
            )
        }

        verifyWithContext(mockSerialisationManager, context).deserialise(
                eq(instructionToken),
                eq(mockBlob),
                eq(metadata)
        )

        verifyWithContext(target, context).clearCache(eq(instructionToken))

        //TODO test clear cache onError
    }

    protected open fun prepareGetCachedResponse(context: String,
                                                operation: Cache,
                                                instructionToken: InstructionToken,
                                                hasResponse: Boolean,
                                                isStale: Boolean,
                                                isCompressed: Int,
                                                isEncrypted: Int,
                                                cacheDateTimeStamp: Long,
                                                expiryDate: Long) = Unit

    protected abstract fun verifyGetCachedResponse(context: String,
                                                   operation: Cache,
                                                   instructionToken: InstructionToken,
                                                   hasResponse: Boolean,
                                                   isStale: Boolean,
                                                   cachedResponse: ResponseWrapper<*, *, Glitch>?)
}
