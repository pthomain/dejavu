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

package dev.pthomain.android.dejavu.interceptors.response

import com.nhaarman.mockitokotlin2.*
import dev.pthomain.android.dejavu.configuration.error.ErrorFactory
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.EMPTY
import dev.pthomain.android.dejavu.interceptors.error.Glitch
import dev.pthomain.android.dejavu.test.*
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import org.junit.Before
import org.junit.Test

class EmptyResponseFactoryUnitTest {

    private lateinit var mockErrorFactory: ErrorFactory<Glitch>

    private lateinit var target: EmptyResponseFactory<Glitch>

    //TODO update test for DONE and EMPTY

    @Before
    fun setUp() {
        mockErrorFactory = mock()
        target = EmptyResponseFactory(mockErrorFactory)
    }

    @Test
    fun testCreateMergeOnNextOnErrorTrue() {
        assertTrueWithContext(
                target.create(
                        true,
                        TestResponse::class.java
                ) is TestResponse,
                "Factory should return an instance of TestResponse"
        )
    }

    @Test
    fun testCreateMergeOnNextOnErrorFalse() {
        assertNullWithContext(
                target.create(
                        false,
                        TestResponse::class.java
                ),
                "Factory should return null when mergeOnNextOnError is false"
        )
    }

    @Test
    fun testEmptyResponseWrapperObservable() {
        val instructionToken = instructionToken()
        val mockError = mock<Glitch>()

        whenever(mockErrorFactory.getError(any())).thenReturn(mockError)

        val wrapper = target.emptyResponseWrapperSingle(
                instructionToken
        ).blockingGet()

        val captor = argumentCaptor<NoSuchElementException>()
        verify(mockErrorFactory).getError(captor.capture())
        val capturedException = captor.firstValue

        assertNotNullWithContext(
                capturedException,
                "Wrong exception"
        )

        assertEqualsWithContext(
                TestResponse::class.java,
                wrapper.responseClass,
                "Wrong response class"
        )

        assertNullWithContext(
                wrapper.response,
                "Response should be null"
        )

        val metadata = wrapper.metadata

        assertEqualsWithContext(
                mockError,
                metadata.exception,
                "Exception didn't match"
        )

        assertEqualsWithContext(
                instructionToken.copy(status = EMPTY),
                metadata.cacheToken,
                "Cache token status should be EMPTY"
        )
    }

}