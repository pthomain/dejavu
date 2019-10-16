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

package dev.pthomain.android.dejavu.interceptors.cache

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.*
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.NOT_CACHED
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import dev.pthomain.android.dejavu.test.instructionToken
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.test.operationSequence
import io.reactivex.Observable
import org.junit.Test
import java.util.*

class CacheInterceptorUnitTest {

    private lateinit var mockInstructionToken: CacheToken
    private lateinit var mockMetadata: CacheMetadata<Glitch>
    private lateinit var mockUpstream: Observable<ResponseWrapper<Glitch>>
    private lateinit var mockUpstreamResponseWrapper: ResponseWrapper<Glitch>
    private lateinit var mockReturnedResponseWrapper: ResponseWrapper<Glitch>
    private lateinit var mockReturnedObservable: Observable<ResponseWrapper<Glitch>>

    private val mockStart = 1234L
    private val mockDateFactory: (Long?) -> Date = { Date(1234L) }

    private lateinit var mockCacheManager: CacheManager<Glitch>


    private fun getTarget(isCacheEnabled: Boolean,
                          operation: Operation): CacheInterceptor<Glitch> {
        mockCacheManager = mock()

        mockInstructionToken = instructionToken(operation)
        mockMetadata = CacheMetadata(mockInstructionToken)

        mockUpstreamResponseWrapper = ResponseWrapper(
                TestResponse::class.java,
                mock<TestResponse>(),
                mockMetadata
        )
        mockUpstream = Observable.just(mockUpstreamResponseWrapper)

        mockReturnedResponseWrapper = mock()
        mockReturnedObservable = Observable.just(mockReturnedResponseWrapper)

        return CacheInterceptor(
                mockCacheManager,
                mockDateFactory,
                isCacheEnabled,
                mockInstructionToken,
                mockStart
        )
    }

    @Test
    fun testApplyCacheEnabledFalse() {
        testApply(false)
    }

    @Test
    fun testApplyCacheEnabledTrue() {
        testApply(true)
    }

    private fun testApply(isCacheEnabled: Boolean) {
        operationSequence { operation ->
            val target = getTarget(
                    isCacheEnabled,
                    operation
            )

            if (isCacheEnabled) {
                when (operation) {
                    is Expiring -> prepareGetCachedResponse(operation)
                    is Clear -> prepareClearCache(operation)
                    is Invalidate -> prepareInvalidate()
                }
            }

            val responseWrapper = target.apply(mockUpstream).blockingFirst()

            if (isCacheEnabled) {
                when (operation) {
                    is Expiring,
                    is Clear,
                    is Invalidate -> assertEqualsWithContext(
                            mockReturnedResponseWrapper,
                            responseWrapper,
                            "The returned observable did not match",
                            "Failure for operation $operation"
                    )

                    else -> verifyDoNotCache(
                            operation,
                            isCacheEnabled,
                            responseWrapper
                    )
                }
            } else {
                verifyDoNotCache(
                        operation,
                        isCacheEnabled,
                        responseWrapper
                )
            }
        }
    }

    private fun prepareGetCachedResponse(operation: Expiring) {
        whenever(mockCacheManager.getCachedResponse(
                eq(mockUpstream),
                eq(mockInstructionToken.copy(instruction = mockInstructionToken.instruction.copy(operation = operation))),
                eq(mockStart)
        )).thenReturn(mockReturnedObservable)
    }

    private fun prepareClearCache(operation: Clear) {
        whenever(mockCacheManager.clearCache(
                eq(mockInstructionToken),
                eq(operation.typeToClear),
                eq(operation.clearStaleEntriesOnly)
        )).thenReturn(mockReturnedObservable)
    }

    private fun prepareInvalidate() {
        whenever(mockCacheManager.invalidate(
                eq(mockInstructionToken)
        )).thenReturn(mockReturnedObservable)
    }

    private fun verifyDoNotCache(operation: Operation,
                                 isCacheEnabled: Boolean,
                                 responseWrapper: ResponseWrapper<Glitch>) {
        assertEqualsWithContext(
                mockMetadata.copy(cacheToken = mockInstructionToken.copy(
                        status = NOT_CACHED,
                        cacheDate = Date(1234L),
                        fetchDate = null,
                        expiryDate = null
                )),
                responseWrapper.metadata,
                "Response wrapper metadata didn't match for operation == $operation and isCacheEnabled == $isCacheEnabled"
        )
    }

}
