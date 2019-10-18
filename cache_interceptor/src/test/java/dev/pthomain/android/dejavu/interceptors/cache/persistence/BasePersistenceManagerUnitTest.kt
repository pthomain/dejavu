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

package dev.pthomain.android.dejavu.interceptors.cache.persistence

import com.nhaarman.mockitokotlin2.*
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Expiring
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Invalidate
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Type.INVALIDATE
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Type.REFRESH
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.FRESH
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.STALE
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.test.*
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import org.junit.Test
import java.util.*

internal abstract class BasePersistenceManagerUnitTest<T : PersistenceManager<Glitch>> {

    protected lateinit var mockSerialisationManager: SerialisationManager<Glitch>
    protected lateinit var mockSerialisationManagerFactory: SerialisationManager.Factory<Glitch>
    protected lateinit var mockDateFactory: (Long?) -> Date
    protected lateinit var mockHasher: Hasher
    protected lateinit var mockCacheToken: CacheToken
    protected lateinit var mockResponseWrapper: ResponseWrapper<Glitch>
    protected lateinit var mockMetadata: CacheMetadata<Glitch>

    protected val mockHash = "mockHash"
    protected val mockBlob = byteArrayOf(1, 2, 3, 4, 5, 6, 8, 9)

    protected val currentDateTime = 10000L
    protected val mockFetchDateTime = 1000L
    protected val mockCacheDateTime = 100L
    protected val mockExpiryDateTime = 10L
    protected val durationInMillis = 5L
    protected val mockCurrentDate = Date(currentDateTime)
    protected val mockFetchDate = Date(mockFetchDateTime)
    protected val mockCacheDate = Date(mockCacheDateTime)
    protected val mockExpiryDate = Date(mockExpiryDateTime)

    protected fun setUpConfiguration(encryptDataGlobally: Boolean,
                                     compressDataGlobally: Boolean,
                                     cacheInstruction: CacheInstruction<*>?): DejaVuConfiguration<Glitch> {
        mockSerialisationManager = mock()
        mockDateFactory = mock()
        mockHasher = mock()
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

        val mockConfiguration = mock<DejaVuConfiguration<Glitch>>()
        whenever(mockConfiguration.compress).thenReturn(compressDataGlobally)
        whenever(mockConfiguration.encrypt).thenReturn(encryptDataGlobally)
        whenever(mockConfiguration.cacheDurationInMillis).thenReturn(durationInMillis)
        whenever(mockConfiguration.logger).thenReturn(mock())

        return mockConfiguration
    }

    protected abstract fun setUp(instructionToken: CacheToken,
                                 encryptDataGlobally: Boolean,
                                 compressDataGlobally: Boolean,
                                 cacheInstruction: CacheInstruction<*>?): T

    @Test
    fun testClearCache() {
        trueFalseSequence { useTypeToClear ->
            trueFalseSequence { clearStaleEntriesOnly ->
                testClearCache(
                        useTypeToClear,
                        clearStaleEntriesOnly
                )
            }
        }
    }

    private fun testClearCache(useTypeToClear: Boolean,
                               clearStaleEntriesOnly: Boolean) {
        val context = "useTypeToClear = $useTypeToClear\nclearStaleEntriesOnly = $clearStaleEntriesOnly"
        val typeToClearClass: Class<*>? = if (useTypeToClear) TestResponse::class.java else null
        val mockClassHash = "mockHash"

        val target = setUp(
                instructionToken(Operation.Clear(typeToClearClass, clearStaleEntriesOnly)),
                true,
                true,
                null
        )

        if (typeToClearClass != null) {
            whenever(mockHasher.hash(eq(typeToClearClass.name))).thenReturn(mockClassHash)
        }

        prepareClearCache(
                context,
                useTypeToClear,
                clearStaleEntriesOnly,
                mockClassHash
        )

        target.clearCache(
                typeToClearClass,
                clearStaleEntriesOnly
        )

        verifyClearCache(
                context,
                useTypeToClear,
                clearStaleEntriesOnly,
                mockClassHash
        )
    }

    protected open fun prepareClearCache(context: String,
                                         useTypeToClear: Boolean,
                                         clearStaleEntriesOnly: Boolean,
                                         mockClassHash: String) = Unit

    protected abstract fun verifyClearCache(context: String,
                                            useTypeToClear: Boolean,
                                            clearStaleEntriesOnly: Boolean,
                                            mockClassHash: String)

    @Test
    fun testCache() {
        var iteration = 0
        operationSequence { operation ->
            if (operation is Expiring) {
                trueFalseSequence { encryptDataGlobally ->
                    trueFalseSequence { compressDataGlobally ->
                        trueFalseSequence { hasPreviousResponse ->
                            trueFalseSequence { isSerialisationSuccess ->
                                testCache(
                                        iteration++,
                                        operation,
                                        encryptDataGlobally,
                                        compressDataGlobally,
                                        hasPreviousResponse,
                                        isSerialisationSuccess
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun testCache(iteration: Int,
                          operation: Expiring,
                          encryptDataGlobally: Boolean,
                          compressDataGlobally: Boolean,
                          hasPreviousResponse: Boolean,
                          isSerialisationSuccess: Boolean) {
        val context = "iteration = $iteration,\n" +
                "operation = $operation,\n" +
                "encryptDataGlobally = $encryptDataGlobally,\n" +
                "compressDataGlobally = $compressDataGlobally,\n" +
                "hasPreviousResponse = $hasPreviousResponse\n" +
                "isSerialisationSuccess = $isSerialisationSuccess"

        val instructionToken = instructionTokenWithHash(operation)

        val target = setUp(
                instructionToken,
                encryptDataGlobally,
                compressDataGlobally,
                instructionToken.instruction
        )

        prepareCache(
                iteration,
                operation,
                encryptDataGlobally,
                compressDataGlobally,
                hasPreviousResponse,
                isSerialisationSuccess
        )

        whenever(mockResponseWrapper.metadata.cacheToken.requestMetadata).thenReturn(instructionToken.requestMetadata)
        val mockPreviousResponse = if (hasPreviousResponse) mock<ResponseWrapper<Glitch>>() else null

        val duration = operation.durationInMillis ?: durationInMillis

        if (mockPreviousResponse != null) {
            val previousMetadata = CacheMetadata<Glitch>(
                    instructionToken(),
                    null,
                    CacheMetadata.Duration(0, 0, 0)
            )
            whenever(mockPreviousResponse.metadata).thenReturn(previousMetadata)
        }

        val encryptData = mockPreviousResponse?.metadata?.cacheToken?.isEncrypted
                ?: operation.encrypt
                ?: encryptDataGlobally

        val compressData = mockPreviousResponse?.metadata?.cacheToken?.isCompressed
                ?: operation.compress
                ?: compressDataGlobally

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
                isSerialisationSuccess,
                duration
        )
    }

    protected open fun prepareCache(iteration: Int,
                                    operation: Expiring,
                                    encryptDataGlobally: Boolean,
                                    compressDataGlobally: Boolean,
                                    hasPreviousResponse: Boolean,
                                    isSerialisationSuccess: Boolean) = Unit

    protected abstract fun verifyCache(context: String,
                                       iteration: Int,
                                       instructionToken: CacheToken,
                                       operation: Expiring,
                                       encryptData: Boolean,
                                       compressData: Boolean,
                                       hasPreviousResponse: Boolean,
                                       isSerialisationSuccess: Boolean,
                                       duration: Long)

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
                    instruction = instructionToken.instruction.copy(operation = Invalidate)
            )))
        }
    }

    private fun prepareAllInvalidation(operation: Operation,
                                       andThen: (String, CacheToken, T) -> Unit) {
        val context = "operation = $operation"
        val instructionToken = instructionTokenWithHash(operation)

        val target = spy(setUp(
                instructionToken,
                true,
                true,
                null
        ))


        andThen(context, instructionToken, target)
    }

    protected abstract fun prepareInvalidate(context: String,
                                             operation: Operation,
                                             instructionToken: CacheToken)

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
                                                instructionToken: CacheToken) = Unit

    protected abstract fun verifyCheckInvalidation(context: String,
                                                   operation: Operation,
                                                   instructionToken: CacheToken)

    @Test
    fun testGetCachedResponse() {
        var iteration = 0
        operationSequence { operation ->
            if (operation is Expiring) {
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

    private fun instructionTokenWithHash(operation: Operation): CacheToken {
        val defaultToken = instructionToken(operation)
        return defaultToken.copy(
                requestMetadata = defaultToken.requestMetadata.copy(urlHash = mockHash)
        )
    }

    private fun testGetCachedResponse(iteration: Int,
                                      operation: Expiring,
                                      hasResponse: Boolean,
                                      isStale: Boolean) {
        val context = "iteration = $iteration,\n" +
                "operation = $operation,\n" +
                "hasResponse = $hasResponse,\n" +
                "isStale = $isStale"

        val instructionToken = instructionTokenWithHash(operation)

        val isCompressed = 1
        val isEncrypted = 1

        val target = spy(setUp(
                instructionToken,
                true,
                true,
                instructionToken.instruction
        ))

        val isDataStale = isStale || operation.type == REFRESH || operation.type == INVALIDATE
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

        val mockResponseWrapper = ResponseWrapper<Glitch>(
                TestResponse::class.java,
                null,
                mock()
        )

        val metadata = SerialisationDecorationMetadata(
                eq(isCompressed == 1),
                eq(isEncrypted == 1)
        )

        whenever(mockSerialisationManager.deserialise(
                eq(instructionToken),
                eq(mockBlob),
                eq(metadata)
        )).thenReturn(if (hasResponse) mockResponseWrapper else null)

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
            val actualMetadata = cachedResponse!!.metadata

            assertEqualsWithContext(
                    CacheMetadata.Duration(0, 0, 0),
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

        verifyWithContext(target, context).clearCache(
                isNull(),
                eq(false)
        )

        //TODO test clear cache onError
    }

    protected open fun prepareGetCachedResponse(context: String,
                                                operation: Expiring,
                                                instructionToken: CacheToken,
                                                hasResponse: Boolean,
                                                isStale: Boolean,
                                                isCompressed: Int,
                                                isEncrypted: Int,
                                                cacheDateTimeStamp: Long,
                                                expiryDate: Long) = Unit

    protected abstract fun verifyGetCachedResponse(context: String,
                                                   operation: Expiring,
                                                   instructionToken: CacheToken,
                                                   hasResponse: Boolean,
                                                   isStale: Boolean,
                                                   cachedResponse: ResponseWrapper<Glitch>?)

}