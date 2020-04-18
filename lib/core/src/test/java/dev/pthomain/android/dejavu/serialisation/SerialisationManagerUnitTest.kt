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

package dev.pthomain.android.dejavu.serialisation

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.CacheInstruction
import dev.pthomain.android.dejavu.error.glitch.Glitch
import dev.pthomain.android.dejavu.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.serialisation.decoration.SerialisationDecorator
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import dev.pthomain.android.dejavu.test.expectException
import dev.pthomain.android.dejavu.test.instructionToken
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.test.trueFalseSequence
import org.junit.Before
import org.junit.Test

class SerialisationManagerUnitTest {

    private lateinit var mockByteToStringConverter: (ByteArray) -> String
    private lateinit var mockSerialiser: Serialiser
    private lateinit var mockSerialisationDecorator1: SerialisationDecorator<Glitch>
    private lateinit var mockSerialisationDecorator2: SerialisationDecorator<Glitch>
    private lateinit var mockMetadata: SerialisationDecorationMetadata
    private lateinit var mockErrorFactory: ErrorFactory<Glitch>
    private lateinit var mockInstruction: CacheInstruction
    private lateinit var mockWrapper: ResponseWrapper<*, *, Glitch>
    private lateinit var decoratorList: List<SerialisationDecorator<Glitch>>

    private val mockByteArrayString = "mockByteArrayString"
    private val mockByteArray = "mockByteArrayString".toByteArray()
    private val mockSerialisedByteArray1 = "mockSerialisedByteArray1".toByteArray()
    private val mockSerialisedByteArray2 = "mockSerialisedByteArray2".toByteArray()

    private lateinit var target: SerialisationManager<Glitch>

    @Before
    fun setUp() {
        mockByteToStringConverter = mock()
        mockSerialiser = mock()
        mockErrorFactory = mock()
        mockInstruction = mock()
        mockSerialisationDecorator1 = mock()
        mockSerialisationDecorator2 = mock()
        mockWrapper = mock()
        mockMetadata = mock()
        decoratorList = listOf(
                mockSerialisationDecorator1,
                mockSerialisationDecorator2
        )
        target = SerialisationManager(
                mockErrorFactory,
                mockSerialiser,
                mockByteToStringConverter,
                decoratorList
        )
        //TODO test factory
    }

    @Test
    fun testSerialise() {
        var iteration = 0

        trueFalseSequence { isNullResponse ->
            trueFalseSequence { isStringResponse ->
                trueFalseSequence { serialisationFails ->
                    testSerialise(
                            iteration++,
                            isNullResponse,
                            isStringResponse,
                            serialisationFails
                    )
                }
            }
        }
    }

    private fun testSerialise(iteration: Int,
                              isNullResponse: Boolean,
                              isStringResponse: Boolean,
                              serialisationFails: Boolean) {
        val context = "iteration = $iteration,\n" +
                "isNullResponse = $isNullResponse,\n" +
                "isStringResponse = $isStringResponse,\n" +
                "serialisationFails = $serialisationFails"

        whenever(mockWrapper.response).thenReturn(ifElse(
                isNullResponse,
                null,
                ifElse(isStringResponse, "mockString", mock<TestResponse>())
        ))

        val responseClass = ifElse(
                isStringResponse,
                String::class.java,
                TestResponse::class.java
        )

        whenever(mockWrapper.responseClass).thenReturn(responseClass)

        if (isNullResponse) {
            expectException(
                    SerialisationException::class.java,
                    "Could not serialise the given response",
                    { target.serialise(mockWrapper, mockMetadata) },
                    context
            )
        } else {
            whenever(mockSerialiser.canHandleType(eq(responseClass))).thenReturn(!serialisationFails)

            if (!serialisationFails) {
                whenever(mockSerialiser.serialise(eq(mockByteArray))).thenReturn(mockByteArrayString)
            }

            whenever(mockSerialisationDecorator1.decorateSerialisation(
                    eq(mockWrapper),
                    eq(mockMetadata),
                    eq(mockByteArray)
            )).thenReturn(mockSerialisedByteArray1)

            whenever(mockSerialisationDecorator2.decorateSerialisation(
                    eq(mockWrapper),
                    eq(mockMetadata),
                    eq(mockSerialisedByteArray1)
            )).thenReturn(mockSerialisedByteArray2)

            val result = target.serialise(
                    mockWrapper,
                    mockMetadata
            )

            assertEqualsWithContext(
                    mockSerialisedByteArray2,
                    result,
                    "The returned byte array didn't match",
                    context
            )
        }

        @Test
        fun testDeserialise() {
            whenever(mockSerialisationDecorator2.decorateSerialisation(
                    eq(mockWrapper),
                    eq(mockMetadata),
                    eq(mockByteArray)
            )).thenReturn(mockSerialisedByteArray2)

            whenever(mockSerialisationDecorator1.decorateSerialisation(
                    eq(mockWrapper),
                    eq(mockMetadata),
                    eq(mockSerialisedByteArray2)
            )).thenReturn(mockSerialisedByteArray1)

            whenever(mockByteToStringConverter(eq(mockSerialisedByteArray1)))
                    .thenReturn(mockByteArrayString)

            val mockResponse = mock<TestResponse>()

            whenever(mockSerialiser.deserialise(
                    eq(mockByteArrayString),
                    eq(TestResponse::class.java)
            )).thenReturn(mockResponse)

            val instructionToken = instructionToken()
            val mockCacheMetadata = mock<ResponseMetadata<Glitch>>()

            whenever(mockErrorFactory.newMetadata(eq(instructionToken)))
                    .thenReturn(mockCacheMetadata)

            whenever(mockErrorFactory.newWrapper(
                    eq(responseClass),
                    eq(mockResponse),
                    eq(mockCacheMetadata)
            )).thenReturn(mockWrapper)

            val result = target.deserialise(
                    instructionToken,
                    mockByteArray,
                    mockMetadata
            )

            assertEqualsWithContext(
                    mockWrapper,
                    result,
                    "The returned wrapper didn't match"
            )
        }
    }
}