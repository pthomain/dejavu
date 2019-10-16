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

import com.nhaarman.mockitokotlin2.mock
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction
import dev.pthomain.android.dejavu.injection.integration.component.IntegrationDejaVuComponent
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.error.glitch.ErrorCode
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch.Companion.NON_HTTP_STATUS
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.*
import dev.pthomain.android.dejavu.retrofit.annotations.CacheException
import dev.pthomain.android.dejavu.retrofit.annotations.CacheException.Type.ANNOTATION
import dev.pthomain.android.dejavu.test.BaseIntegrationTest
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import dev.pthomain.android.dejavu.test.assertNotNullWithContext
import dev.pthomain.android.dejavu.test.callAdapterFactory
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import junit.framework.TestCase.assertTrue
import org.junit.Test
import retrofit2.CallAdapter


@Suppress("UNCHECKED_CAST")
internal class ProcessingErrorAdapterIntegrationTest
    : BaseIntegrationTest<ProcessingErrorAdapter.Factory<Glitch>>(IntegrationDejaVuComponent::processingErrorAdapterFactory) {

    private lateinit var cacheException: CacheException
    private lateinit var cacheToken: CacheToken
    private lateinit var testObserver: TestObserver<Any>

    private lateinit var targetAdapter: CallAdapter<Any, Any>

    private fun setUp(rxClass: Class<*>,
                      rxType: AnnotationProcessor.RxType) {
        val defaultAdapter = callAdapterFactory(rxClass, retrofit) { returnType, annotations, retrofit ->
            cacheComponent.defaultAdapterFactory().get(returnType, annotations, retrofit) as CallAdapter<Any, Any>
        }

        cacheException = CacheException(
                ANNOTATION,
                "error"
        )

        cacheToken = instructionToken(CacheInstruction.Operation.DoNotCache)

        targetAdapter = target.create(
                defaultAdapter,
                cacheToken,
                1234L,
                rxType,
                cacheException
        ) as CallAdapter<Any, Any>

        enqueueResponse("", 200)

        testObserver = TestObserver()
    }

    @Test
    fun testAdaptObservable() {
        setUp(Observable::class.java, OBSERVABLE)

        val adapted = targetAdapter.adapt(mock()) as Observable<Any>

        adapted.subscribe(testObserver)
        verifyObserver(testObserver)
    }

    @Test
    fun testAdaptSingle() {
        setUp(Single::class.java, SINGLE)

        val adapted = targetAdapter.adapt(mock()) as Single<Any>

        adapted.subscribe(testObserver)
        verifyObserver(testObserver)
    }

    @Test
    fun testAdaptCompletable() {
        setUp(Completable::class.java, COMPLETABLE)

        val adapted = targetAdapter.adapt(mock()) as Completable

        adapted.subscribe(testObserver)
        verifyObserver(testObserver, true)
    }

    private fun verifyObserver(testObserver: TestObserver<Any>,
                               isCompletable: Boolean = false) {


        val exception = if (!isCompletable) {
            assertEqualsWithContext(
                    1,
                    testObserver.values().size,
                    "Observer should have one value"
            )

            val response = testObserver.values().first() as TestResponse

            assertNotNullWithContext(
                    response.metadata,
                    "Response should have metadata"
            )

            assertNotNullWithContext(
                    response.metadata.cacheToken,
                    "Response metadata should have a cache token"
            )

            assertNotNullWithContext(
                    response.metadata.exception,
                    "Response metadata should have an exception"
            )

            assertNotNullWithContext(
                    response.metadata.callDuration,
                    "Response metadata should have a call duration"
            )

            assertEqualsWithContext(
                    cacheToken,
                    response.metadata.cacheToken,
                    "Cache token should match instruction token"
            )

            response.metadata.exception
        } else {
            assertEqualsWithContext(
                    1,
                    testObserver.errors().size,
                    "Observer should have one error"
            )

            testObserver.errors().first()
        }

        assertTrue(
                "Exception should be a Glitch",
                exception is Glitch
        )

        val glitch = exception as Glitch

        assertEqualsWithContext(
                ErrorCode.CONFIG,
                glitch.errorCode,
                "Glitch error code should be CONFIG"
        )

        assertEqualsWithContext(
                "Configuration error",
                glitch.description,
                "Glitch error description didn't match"
        )

        assertEqualsWithContext(
                NON_HTTP_STATUS,
                glitch.httpStatus,
                "Glitch error HTTP status didn't match"
        )

        val cause = glitch.cause

        assertNotNullWithContext(
                cause,
                "Glitch cause should not be null"
        )

        assertTrue(
                "Glitch cause should be CacheException",
                cause is CacheException
        )

        val cacheException = cause as CacheException

        assertEqualsWithContext(
                CacheException.Type.ANNOTATION,
                cacheException.type,
                "Cache exception type should be ANNOTATION"
        )

        assertEqualsWithContext(
                "error",
                cacheException.message,
                "Cache exception message should be error"
        )
    }
}