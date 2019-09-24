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

import dev.pthomain.android.dejavu.configuration.CacheInstruction
import dev.pthomain.android.dejavu.test.operationSequence
import dev.pthomain.android.dejavu.test.trueFalseSequence
import org.junit.Test
import java.util.*

abstract class BasePersistenceManagerUnitTest {

    protected val currentDateTime = 10000L
    protected val mockFetchDateTime = 1000L
    protected val mockCacheDateTime = 100L
    protected val mockExpiryDateTime = 10L
    protected val durationInMillis = 5L
    protected val mockCurrentDate = Date(currentDateTime)
    protected val mockFetchDate = Date(mockFetchDateTime)
    protected val mockCacheDate = Date(mockCacheDateTime)
    protected val mockExpiryDate = Date(mockExpiryDateTime)

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

    protected abstract fun testClearCache(useTypeToClear: Boolean,
                                          clearStaleEntriesOnly: Boolean)

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