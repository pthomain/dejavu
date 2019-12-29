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

package dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.file

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationException
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.BaseSerialisationDecoratorUnitTest
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.test.assertByteArrayEqualsWithContext
import dev.pthomain.android.dejavu.test.expectException
import dev.pthomain.android.dejavu.test.trueFalseSequence
import dev.pthomain.android.dejavu.test.withContext
import org.junit.Before

class FileSerialisationDecoratorUnitTest : BaseSerialisationDecoratorUnitTest() {

    private lateinit var mockByteToStringConverter: (ByteArray) -> String

    private lateinit var target: FileSerialisationDecorator<Glitch>

    @Before
    override fun setUp() {
        super.setUp()
        mockByteToStringConverter = mock()

        whenever(mockByteToStringConverter.invoke(eq(mockPayload))).thenReturn(mockSerialisedPayload)

        target = FileSerialisationDecorator(
                mockByteToStringConverter
        )
    }

    override fun testDecorateSerialisation(context: String,
                                           useString: Boolean,
                                           metadata: SerialisationDecorationMetadata,
                                           mockWrapper: ResponseWrapper<Glitch>) {
        val result = target.decorateSerialisation(
                mockWrapper,
                mock(),
                mockPayload
        )

        val responseClassName = ifElse(
                useString,
                "java.lang.String",
                "dev.pthomain.android.dejavu.test.network.model.TestResponse"
        )

        assertByteArrayEqualsWithContext(
                "$responseClassName\n$mockSerialisedPayload".toByteArray(),
                result,
                withContext("The serialised byte array did not match", context)
        )
    }

    override fun testDecorateDeserialisation(context: String,
                                             metadata: SerialisationDecorationMetadata) {
        trueFalseSequence { deserialisationFails ->
            val newContext = "$context,\ndeserialisationFails = $deserialisationFails"
            val validString = "java.lang.String\n$mockSerialisedPayload"
            val validArray = validString.toByteArray()

            val payload = ifElse(
                    deserialisationFails,
                    mockPayload,
                    validArray
            )

            if (deserialisationFails) {
                expectException(
                        SerialisationException::class.java,
                        "Could not extract the payload",
                        {
                            target.decorateSerialisation(
                                    mock(),
                                    mock(),
                                    payload
                            )
                        },
                        newContext
                )
            } else {
                whenever(mockByteToStringConverter.invoke(eq(payload))).thenReturn(validString)

                val result = target.decorateSerialisation(
                        mock(),
                        mock(),
                        payload
                )

                assertByteArrayEqualsWithContext(
                        mockSerialisedPayloadArray,
                        result,
                        withContext("Deserialised byte array did not match", newContext)
                )
            }
        }
    }
}