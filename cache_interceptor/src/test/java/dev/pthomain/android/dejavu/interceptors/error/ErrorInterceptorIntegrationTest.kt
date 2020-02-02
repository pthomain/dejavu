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

package dev.pthomain.android.dejavu.interceptors.error

import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.injection.integration.component.IntegrationDejaVuComponent
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.EMPTY
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseWrapperFactory.EmptyResponseException
import dev.pthomain.android.dejavu.test.*
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.utils.Utils.swapLambdaWhen
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test

internal class ErrorInterceptorIntegrationTest
    : BaseIntegrationTest<ErrorInterceptor.Factory<Glitch>>(IntegrationDejaVuComponent::errorInterceptorFactory) {

    private lateinit var targetErrorInterceptor: ErrorInterceptor<*, *, Glitch>

    @Before
    override fun setUp() {
        super.setUp()
        targetErrorInterceptor = target.create(
                instructionToken(Cache())
        )
    }

    private fun apply(observable: Observable<Any>) =
            targetErrorInterceptor.apply(observable).blockingFirst()

    @Test
    fun testApplyWithResponse() {
        val value = Object()

        val result = apply(Observable.just(value))

        assertEqualsWithContext(
                TestResponse::class.java,
                result.responseClass,
                "Response class didn't match"
        )

        assertEqualsWithContext(
                TestResponse::class.java,
                result.responseClass,
                "Response didn't match"
        )

        val metadata = result.metadata

        assertNullWithContext(
                metadata.exception,
                "Metadata should not have an exception"
        )

        assertEqualsWithContext(
                instructionToken(Cache()),
                metadata.cacheToken,
                "Cache token didn't match"
        )
    }

    @Test
    fun testApplyWithEmpty() {
        testApplyWithException(
                Observable.empty(),
                NoSuchElementException("Response was empty")
        )
    }

    @Test
    fun testApplyWithError() {
        val error = NullPointerException("error")
        testApplyWithException(
                Observable.error(error),
                error
        )
    }

    private fun testApplyWithException(observable: Observable<Any>,
                                       exception: Throwable) {
        val result = apply(observable)

        assertEqualsWithContext(
                TestResponse::class.java,
                result.responseClass,
                "Response class didn't match"
        )

        assertNullWithContext(
                result.response,
                "Response should be null"
        )

        val metadata = result.metadata

        assertNotNullWithContext(
                metadata.exception,
                "Metadata should have an exception"
        )

        assertTrueWithContext(
                metadata.exception is Glitch,
                "Exception should be Glitch"
        )

        val glitch = metadata.exception as Glitch

        val expectedException = ifElse(
                exception is NoSuchElementException,
                EmptyResponseException,
                exception
        )

        assertGlitchWithContext(
                Glitch(expectedException),
                glitch,
                "Glitch didn't match"
        )

        assertEqualsWithContext(
                instructionToken(Cache()).swapLambdaWhen(exception is NoSuchElementException) {
                    ResponseToken(
                            it!!.instruction,
                            EMPTY,
                            NOW,
                            null,
                            null
                    )
                },
                metadata.cacheToken,
                "Cache token didn't match"
        )
    }
}