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

package dev.pthomain.android.dejavu.serialisation.decoration.compression

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.dejavu.error.glitch.Glitch
import dev.pthomain.android.dejavu.serialisation.decoration.BaseSerialisationDecoratorUnitTest
import dev.pthomain.android.dejavu.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import dev.pthomain.android.dejavu.test.instructionToken
import org.junit.Before

class CompressionSerialisationDecoratorUnitTest : BaseSerialisationDecoratorUnitTest() {

    private lateinit var mockCompresser: Function1<ByteArray, ByteArray>
    private lateinit var mockUncompresser: Function3<ByteArray, Int, Int, ByteArray>

    private lateinit var target: dev.pthomain.android.dejavu.serialisation.compression.CompressionSerialisationDecorator<Glitch>

    @Before
    override fun setUp() {
        super.setUp()
        mockCompresser = mock()
        mockUncompresser = mock()

        target = dev.pthomain.android.dejavu.serialisation.compression.CompressionSerialisationDecorator(
                mock(),
                mockCompresser,
                mockUncompresser
        )
    }

    override fun testDecorateSerialisation(context: String,
                                           useString: Boolean,
                                           metadata: SerialisationDecorationMetadata,
                                           mockWrapper: ResponseWrapper<*, *, Glitch>) {
        val expectedResult = if (metadata.isCompressed) {
            whenever(mockCompresser.invoke(eq(mockPayload))).thenReturn(mockSerialisedPayloadArray)
            mockSerialisedPayloadArray
        } else mockPayload

        assertEqualsWithContext(
                expectedResult,
                target.decorateSerialisation(
                        mockWrapper,
                        metadata,
                        mockPayload
                ),
                "The returned payload didn't match",
                context
        )
    }

    override fun testDecorateDeserialisation(context: String,
                                             metadata: SerialisationDecorationMetadata) {
        val expectedResult = if (metadata.isCompressed) {
            whenever(mockUncompresser.invoke(
                    eq(mockPayload),
                    eq(0),
                    eq(mockPayload.size)
            )).thenReturn(mockSerialisedPayloadArray)
            mockSerialisedPayloadArray
        } else mockPayload

        assertEqualsWithContext(
                expectedResult,
                target.decorateDeserialisation(
                        instructionToken(),
                        metadata,
                        mockPayload
                ),
                "The returned payload didn't match",
                context
        )
    }

}