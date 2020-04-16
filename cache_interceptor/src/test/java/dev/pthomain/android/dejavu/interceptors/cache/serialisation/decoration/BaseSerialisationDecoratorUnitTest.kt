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

package dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration

import androidx.annotation.CallSuper
import com.nhaarman.mockitokotlin2.mock
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.test.trueFalseSequence
import org.junit.Before
import org.junit.Test

abstract class BaseSerialisationDecoratorUnitTest {

    private lateinit var mockWrapper: ResponseWrapper<*, *, Glitch>

    protected val mockStringResponse = "mockStringResponse"
    protected lateinit var mockResponse: TestResponse

    protected val mockPayload: ByteArray = "123456".toByteArray()
    protected val mockSerialisedPayload = "mockSerialisedPayload"
    protected val mockSerialisedPayloadArray = mockSerialisedPayload.toByteArray()

    @Before
    @CallSuper
    open fun setUp() {
        mockResponse = mock()
    }

    @Test
    fun testDecorateSerialisation() {
        var iteration = 0
        trueFalseSequence { useString ->
            trueFalseSequence { isCompressed ->
                trueFalseSequence { isEncrypted ->
                    val context = "iteration = ${iteration++},\n" +
                            "useString = $useString,\n" +
                            "isCompressed = $isCompressed,\n" +
                            "isEncrypted = $isEncrypted"

                    mockWrapper = ResponseWrapper(
                            ifElse(useString, String::class.java, TestResponse::class.java),
                            ifElse(useString, mockStringResponse, mockResponse),
                            mock()
                    )

                    testDecorateSerialisation(
                            context,
                            useString,
                            SerialisationDecorationMetadata(isCompressed, isEncrypted),
                            mockWrapper
                    )
                }
            }
        }
    }

    abstract fun testDecorateSerialisation(context: String,
                                           useString: Boolean,
                                           metadata: SerialisationDecorationMetadata,
                                           mockWrapper: ResponseWrapper<*, *, Glitch>)

    @Test
    fun testDecorateDeserialisation() {
        var iteration = 0
        trueFalseSequence { isCompressed ->
            trueFalseSequence { isEncrypted ->
                val context = "iteration = ${iteration++},\n" +
                        "isCompressed = $isCompressed,\n" +
                        "isEncrypted = $isEncrypted"
            }
        }
    }

    abstract fun testDecorateDeserialisation(context: String,
                                             metadata: SerialisationDecorationMetadata)

}