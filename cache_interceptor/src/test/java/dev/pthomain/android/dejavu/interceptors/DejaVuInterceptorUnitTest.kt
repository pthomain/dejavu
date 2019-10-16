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

package dev.pthomain.android.dejavu.interceptors

import com.nhaarman.mockitokotlin2.*
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Expiring
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Type.DO_NOT_CACHE
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.*
import dev.pthomain.android.dejavu.test.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test
import java.util.*

class DejaVuInterceptorUnitTest {

    private val start = 1234L
    private val mockDateFactory: (Long?) -> Date = { Date(start) }

    private lateinit var mockErrorInterceptorFactory: (CacheToken, Long) -> ObservableTransformer<Any, ResponseWrapper<Glitch>>
    private lateinit var mockCacheInterceptorFactory: (CacheToken, Long) -> ObservableTransformer<ResponseWrapper<Glitch>, ResponseWrapper<Glitch>>
    private lateinit var mockResponseInterceptorFactory: (CacheToken, Boolean, Boolean, Long) -> ObservableTransformer<ResponseWrapper<Glitch>, Any>
    private lateinit var mockConfiguration: DejaVuConfiguration<Glitch>
    private lateinit var mockHasher: Hasher
    private lateinit var mockRequestMetadata: RequestMetadata.UnHashed
    private lateinit var mockHashedMetadata: RequestMetadata.Hashed
    private lateinit var mockErrorInterceptor: ObservableTransformer<Any, ResponseWrapper<Glitch>>
    private lateinit var mockCacheInterceptor: ObservableTransformer<ResponseWrapper<Glitch>, ResponseWrapper<Glitch>>
    private lateinit var mockResponseInterceptor: ObservableTransformer<ResponseWrapper<Glitch>, Any>
    private lateinit var mockInstructionToken: CacheToken
    private lateinit var mockUpstreamObservable: Observable<Any>
    private lateinit var mockErrorResponseObservable: Observable<ResponseWrapper<Glitch>>
    private lateinit var mockCacheResponseObservable: Observable<ResponseWrapper<Glitch>>
    private lateinit var mockResponseObservable: Observable<Any>
    private lateinit var errorTokenCaptor: KArgumentCaptor<CacheToken>
    private lateinit var cacheTokenCaptor: KArgumentCaptor<CacheToken>
    private lateinit var responseTokenCaptor: KArgumentCaptor<CacheToken>
    private lateinit var targetFactory: DejaVuInterceptor.Factory<Glitch>

    private fun setUp(operation: CacheInstruction.Operation,
                      rxType: AnnotationProcessor.RxType): DejaVuInterceptor<Glitch> {
        mockErrorInterceptorFactory = mock()
        mockCacheInterceptorFactory = mock()
        mockResponseInterceptorFactory = mock()
        mockConfiguration = mock()
        mockHasher = mock()

        mockErrorInterceptor = ObservableTransformer { mockErrorResponseObservable }
        mockCacheInterceptor = ObservableTransformer { mockCacheResponseObservable }
        mockResponseInterceptor = ObservableTransformer { mockResponseObservable }

        mockRequestMetadata = defaultRequestMetadata()
        mockHashedMetadata = mock()

        mockInstructionToken = instructionToken(operation)
        mockUpstreamObservable = Observable.just("" as Any)
        mockErrorResponseObservable = Observable.just(mock())
        mockCacheResponseObservable = Observable.just(mock())
        mockResponseObservable = Observable.just("" as Any)

        whenever(mockHasher.hash(eq(mockRequestMetadata))).thenReturn(mock())

        targetFactory = DejaVuInterceptor.Factory(
                mockHasher,
                mockDateFactory,
                mockErrorInterceptorFactory,
                mockCacheInterceptorFactory,
                mockResponseInterceptorFactory,
                mockConfiguration
        )

        errorTokenCaptor = argumentCaptor()
        cacheTokenCaptor = argumentCaptor()
        responseTokenCaptor = argumentCaptor()

        whenever(mockErrorInterceptorFactory.invoke(
                errorTokenCaptor.capture(),
                eq(start)
        )).thenReturn(mockErrorInterceptor)

        whenever(mockCacheInterceptorFactory.invoke(
                cacheTokenCaptor.capture(),
                eq(start)
        )).thenReturn(mockCacheInterceptor)

        whenever(mockResponseInterceptorFactory.invoke(
                responseTokenCaptor.capture(),
                eq(rxType == SINGLE),
                eq(rxType == COMPLETABLE),
                eq(start)
        )).thenReturn(mockResponseInterceptor)

        whenever(mockHasher.hash(mockRequestMetadata)).thenReturn(mockHashedMetadata)

        whenever(mockConfiguration.compress).thenReturn(true)
        whenever(mockConfiguration.encrypt).thenReturn(true)

        return targetFactory.create(
                mockInstructionToken.instruction,
                mockRequestMetadata
        ) as DejaVuInterceptor<Glitch>
    }

    @Test
    fun testApplyObservable() {
        testApply(OBSERVABLE)
    }

    @Test
    fun testApplySingle() {
        testApply(SINGLE)
    }

    @Test
    fun testApplyCompletable() {
        testApply(COMPLETABLE)
    }

    private fun testApply(rxType: AnnotationProcessor.RxType) {
        operationSequence { operation ->
            trueFalseSequence { isCacheEnabled ->
                val target = setUp(operation, rxType)
                val testObserver = TestObserver<Any>()

                val context = "Operation = $operation,\nisCacheEnabled = $isCacheEnabled"

                when (rxType) {
                    OBSERVABLE -> target.apply(mockUpstreamObservable).subscribe(testObserver)
                    SINGLE -> target.apply(Single.just("" as Any)).subscribe(testObserver)
                    COMPLETABLE -> target.apply(Completable.complete()).subscribe(testObserver)
                }

                val errorToken = errorTokenCaptor.firstValue
                val cacheToken = cacheTokenCaptor.firstValue
                val responseToken = responseTokenCaptor.firstValue

                assertEqualsWithContext(
                        errorToken,
                        cacheToken,
                        "Error token and cache token should be the same",
                        context
                )

                assertEqualsWithContext(
                        cacheToken,
                        responseToken,
                        "Response token and cache token should be the same",
                        context
                )

                if (!isCacheEnabled) {
                    assertTrueWithContext(
                            errorToken.instruction.operation.type == DO_NOT_CACHE,
                            "Cache token should be DO_NOT_CACHE when isCacheEnabled == false",
                            context
                    )
                }

                assertEqualsWithContext(
                        mockHashedMetadata,
                        errorToken.requestMetadata,
                        "Request metadata didn't match",
                        context
                )

                if (operation is Expiring) {
                    if (operation.compress == null) {
                        assertTrueWithContext(
                                errorToken.isCompressed,
                                "Token value for isCompressed should be true",
                                context
                        )
                    } else {
                        assertEqualsWithContext(
                                operation.compress,
                                errorToken.isCompressed,
                                "Token value for isCompressed didn't match operation's value",
                                context
                        )
                    }

                    if (operation.encrypt == null) {
                        assertTrueWithContext(
                                errorToken.isEncrypted,
                                "Token value for isEncrypted should be true",
                                context
                        )
                    } else {
                        assertEqualsWithContext(
                                operation.encrypt,
                                errorToken.isEncrypted,
                                "Token value for isEncrypted didn't match operation's value",
                                context
                        )
                    }
                } else {
                    assertTrueWithContext(
                            errorToken.isCompressed,
                            "Token value for isCompressed should be true",
                            context

                    )
                    assertTrueWithContext(
                            errorToken.isEncrypted,
                            "Token value for isEncrypted should be true",
                            context
                    )
                }
            }
        }
    }
}