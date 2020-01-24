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

package dev.pthomain.android.dejavu.retrofit

import com.nhaarman.mockitokotlin2.*
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CacheInstruction
import dev.pthomain.android.dejavu.interceptors.cache.instruction.RequestMetadata.Companion.DEFAULT_URL
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.DoNotCache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.annotations.CacheException
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import dev.pthomain.android.dejavu.test.assertFalseWithContext
import dev.pthomain.android.dejavu.test.callAdapterFactory
import dev.pthomain.android.dejavu.test.instructionToken
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import org.junit.Before
import org.junit.Test
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.lang.reflect.Type
import java.util.*

class RetrofitCallAdapterFactoryUnitTest {

    private lateinit var mockRxJava2CallAdapterFactory: RxJava2CallAdapterFactory
    private lateinit var mockDejaVuFactory: DejaVuInterceptor.Factory<Glitch>
    private lateinit var mockAnnotationProcessor: AnnotationProcessor<Glitch>
    private lateinit var mockDefaultCallAdapter: CallAdapter<Any, Any>
    private lateinit var mockAnnotations: Array<Annotation>
    private lateinit var mockRetrofit: Retrofit
    private lateinit var mockReturnType: Type
    private lateinit var mockOperation: Operation
    private lateinit var mockException: CacheException
    private lateinit var mockInnerFactory: (DejaVuInterceptor.Factory<Glitch>, String, Class<*>, Operation?, CallAdapter<Any, Any>) -> CallAdapter<*, *>
    private lateinit var mockReturnedAdapter: CallAdapter<*, *>

    private val mockDateFactory: (Long?) -> Date = { Date(1234L) }

    private lateinit var targetFactory: RetrofitCallAdapterFactory<Glitch>

    @Before
    fun setUp() {
        mockRxJava2CallAdapterFactory = mock()
        mockDejaVuFactory = mock()
        mockAnnotationProcessor = mock()
        mockDefaultCallAdapter = mock()
        mockRetrofit = mock()
        mockOperation = instructionToken().instruction.operation
        mockException = mock()
        mockInnerFactory = mock()
        mockReturnedAdapter = mock()

        targetFactory = RetrofitCallAdapterFactory(
                mock(),
                mockRxJava2CallAdapterFactory,
                mockInnerFactory,
                mockDateFactory,
                mockDejaVuFactory,
                mock(),
                mock(),
                mock(),
                mock()
        )
    }


    private fun testFactory(rxType: RxType,
                            throwAnnotationException: Boolean = false) {
        val responseClass = if (rxType == WRAPPABLE) Any::class.java
        else TestResponse::class.java

        callAdapterFactory(rxType.rxClass, mockRetrofit, responseClass) { returnType, annotations, _ ->
            mockReturnType = returnType
            mockAnnotations = annotations

            whenever(mockRxJava2CallAdapterFactory.get(
                    eq(returnType),
                    eq(mockAnnotations),
                    eq(mockRetrofit)
            )).thenReturn(mockDefaultCallAdapter)

            mockDefaultCallAdapter
        }

        whenever(mockAnnotationProcessor.process(
                eq(mockAnnotations),
                eq(rxType),
                eq(responseClass)
        )).also {
            if (throwAnnotationException)
                it.thenThrow(mockException)
            else
                it.thenReturn(mockOperation)
        }

        val cacheTokenCaptor = argumentCaptor<CacheToken>()

        whenever(mockInnerFactory.invoke(
                eq(mockDejaVuFactory),
                eq("method returning " + rxType.getTypedName(responseClass)),
                eq(responseClass),
                eq(mockOperation),
                eq(mockDefaultCallAdapter)
        )).thenReturn(mockReturnedAdapter)

        assertEqualsWithContext(
                ifElse(throwAnnotationException, mockDefaultCallAdapter, mockReturnedAdapter),
                targetFactory.get(
                        mockReturnType,
                        mockAnnotations,
                        mockRetrofit
                ),
                "Factory didn't return the right adapter"
        )

        if (throwAnnotationException) {
            verify(mockInnerFactory, never()).invoke(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
            )

            val token = cacheTokenCaptor.firstValue

            assertEqualsWithContext(
                    CacheInstruction(token.instruction.requestMetadata, DoNotCache),
                    token.instruction,
                    "Exception cache token instruction didn't match"
            )

            assertFalseWithContext(
                    token.isCompressed,
                    "Token isCompressed should be false"
            )

            assertFalseWithContext(
                    token.isEncrypted,
                    "Token isEncrypted should be false"
            )

            assertEqualsWithContext(
                    DEFAULT_URL,
                    token.instruction.requestMetadata.url,
                    "Exception cache token URL should be empty"
            )
        }
    }

    @Test
    fun testGetObservable() {
        testFactory(OBSERVABLE)
    }

    @Test
    fun testGetSingle() {
        testFactory(SINGLE)
    }

    @Test
    fun testGetCompletable() {
        testFactory(WRAPPABLE)
    }

    @Test
    fun testGetObservableWithAnnotationException() {
        testFactory(OBSERVABLE, true)
    }

    @Test
    fun testGetSingleWithAnnotationException() {
        testFactory(SINGLE, true)
    }

    @Test
    fun testGetCompletableWithAnnotationException() {
        testFactory(WRAPPABLE, true)
    }

}