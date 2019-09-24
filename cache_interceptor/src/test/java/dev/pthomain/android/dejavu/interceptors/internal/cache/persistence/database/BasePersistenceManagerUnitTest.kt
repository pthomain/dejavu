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

package dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.database

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.configuration.CacheInstruction
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.internal.error.Glitch
import dev.pthomain.android.dejavu.response.CacheMetadata
import dev.pthomain.android.dejavu.response.ResponseWrapper
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.test.operationSequence
import dev.pthomain.android.dejavu.test.trueFalseSequence
import org.junit.Test
import java.util.*

internal abstract class BasePersistenceManagerUnitTest<T : PersistenceManager<Glitch>> {

    protected lateinit var mockSerialisationManager: SerialisationManager<Glitch>
    protected lateinit var mockDateFactory: (Long?) -> Date
    protected lateinit var mockHasher: Hasher
    protected lateinit var mockCacheToken: CacheToken
    protected lateinit var mockResponseWrapper: ResponseWrapper<Glitch>
    protected lateinit var mockMetadata: CacheMetadata<Glitch>

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
                                     cacheInstruction: CacheInstruction?): CacheConfiguration<Glitch> {
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

        val mockConfiguration = mock<CacheConfiguration<Glitch>>()
        whenever(mockConfiguration.compress).thenReturn(compressDataGlobally)
        whenever(mockConfiguration.encrypt).thenReturn(encryptDataGlobally)
        whenever(mockConfiguration.cacheDurationInMillis).thenReturn(durationInMillis)
        whenever(mockConfiguration.logger).thenReturn(mock())

        return mockConfiguration
    }

    protected abstract fun setUp(encryptDataGlobally: Boolean,
                                 compressDataGlobally: Boolean,
                                 cacheInstruction: CacheInstruction?): T

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
            if (operation is CacheInstruction.Operation.Expiring) {
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

    protected abstract fun testCache(iteration: Int,
                                     operation: CacheInstruction.Operation.Expiring,
                                     encryptDataGlobally: Boolean,
                                     compressDataGlobally: Boolean,
                                     hasPreviousResponse: Boolean,
                                     isSerialisationSuccess: Boolean)

    @Test
    fun testInvalidate() {
        operationSequence { operation ->
            testInvalidate(operation)
        }
    }

    protected abstract fun testInvalidate(operation: CacheInstruction.Operation)

    @Test
    fun testGetCachedResponse() {
        var iteration = 0
        operationSequence { operation ->
            if (operation is CacheInstruction.Operation.Expiring) {
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

    protected abstract fun testGetCachedResponse(iteration: Int,
                                                 operation: CacheInstruction.Operation.Expiring,
                                                 hasResponse: Boolean,
                                                 isStale: Boolean)

}