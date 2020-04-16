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

package dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.encryption

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationException
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.BaseSerialisationDecoratorUnitTest
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.encryption.EncryptionSerialisationDecorator.Companion.DATA_TAG
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import dev.pthomain.android.dejavu.test.expectException
import dev.pthomain.android.dejavu.test.instructionToken
import dev.pthomain.android.dejavu.test.trueFalseSequence
import dev.pthomain.android.mumbo.base.EncryptionManager
import org.junit.Before

class EncryptionSerialisationDecoratorUnitTest : BaseSerialisationDecoratorUnitTest() {

    private lateinit var mockEncryptionManager: EncryptionManager

    private lateinit var target: EncryptionSerialisationDecorator<Glitch>

    @Before
    override fun setUp() {
        super.setUp()
        mockEncryptionManager = mock()
        target = EncryptionSerialisationDecorator(
                mockEncryptionManager
        )
    }

    override fun testDecorateSerialisation(context: String,
                                           useString: Boolean,
                                           metadata: SerialisationDecorationMetadata,
                                           mockWrapper: ResponseWrapper<*, *, Glitch>) {
        prepareEncryption(context, metadata, true)
    }

    override fun testDecorateDeserialisation(context: String,
                                             metadata: SerialisationDecorationMetadata) {
        prepareEncryption(context, metadata, false)
    }

    private fun prepareEncryption(context: String,
                                  metadata: SerialisationDecorationMetadata,
                                  isSerialisation: Boolean) {
        trueFalseSequence { isEncryptionAvailable ->
            trueFalseSequence { encryptionFails ->
                val newContext = "$context\n," +
                        "isEncryptionAvailable = $isEncryptionAvailable\n," +
                        "encryptionFails = $encryptionFails" +
                        "isSerialisation = $isSerialisation"

                whenever(mockEncryptionManager.isEncryptionAvailable).thenReturn(isEncryptionAvailable)

                val call: () -> ByteArray? = if (isSerialisation) {
                    { target.decorateSerialisation(mock(), metadata, mockPayload) }
                } else {
                    { target.decorateDeserialisation(instructionToken(), metadata, mockPayload) }
                }

                if (isEncryptionAvailable && metadata.isEncrypted) {
                    val returnedPayload = ifElse(
                            encryptionFails,
                            null,
                            mockSerialisedPayloadArray
                    )

                    if (isSerialisation) {
                        whenever(mockEncryptionManager.encryptBytes(
                                eq(mockPayload),
                                eq(DATA_TAG),
                                isNull()
                        )).thenReturn(returnedPayload)
                    } else {
                        whenever(mockEncryptionManager.decryptBytes(
                                eq(mockPayload),
                                eq(DATA_TAG)
                        )).thenReturn(returnedPayload)
                    }

                    if (encryptionFails) {
                        expectException(
                                SerialisationException::class.java,
                                ifElse(isSerialisation, "Could not encrypt data", "Could not decrypt data"),
                                { call() },
                                context
                        )
                    } else assertEqualsWithContext(
                            mockSerialisedPayloadArray,
                            call(),
                            "The returned byte array didn't match",
                            newContext
                    )

                } else assertEqualsWithContext(
                        mockPayload,
                        call(),
                        "The returned byte array didn't match",
                        newContext
                )
            }
        }
    }

}