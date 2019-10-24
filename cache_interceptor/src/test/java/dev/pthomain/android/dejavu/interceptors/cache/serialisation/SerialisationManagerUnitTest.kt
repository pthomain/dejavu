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

package dev.pthomain.android.dejavu.interceptors.cache.serialisation

import com.nhaarman.mockitokotlin2.*
import dev.pthomain.android.boilerplate.core.utils.lambda.Action
import dev.pthomain.android.dejavu.configuration.Serialiser
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager.Factory.Type.FILE
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.compression.CompressionSerialisationDecorator
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.encryption.EncryptionSerialisationDecorator
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.file.FileSerialisationDecorator
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import dev.pthomain.android.dejavu.test.assertNullWithContext
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.test.trueFalseSequence
import dev.pthomain.android.dejavu.test.verifyNeverWithContext
import dev.pthomain.android.mumbo.base.EncryptionManager
import org.junit.Test

class SerialisationManagerUnitTest {

    private lateinit var mockEncryptionManager: EncryptionManager
    private lateinit var mockWrapper: ResponseWrapper<Glitch>
    private lateinit var mockSerialiser: Serialiser
    private lateinit var mockFileSerialisationDecorator: FileSerialisationDecorator<Glitch>
    private lateinit var mockCompressionSerialisationDecorator: CompressionSerialisationDecorator<Glitch>
    private lateinit var mockEncryptionSerialisationDecorator: EncryptionSerialisationDecorator<Glitch>
    private lateinit var mockCompresser: Function1<ByteArray, ByteArray>
    private lateinit var mockUncompresser: Function3<ByteArray, Int, Int, ByteArray>
    private lateinit var mockByteToStringConverter: (ByteArray) -> String
    private lateinit var mockOnError: Action
    private lateinit var mockInstructionToken: CacheToken
    private lateinit var mockInstruction: CacheInstruction
    private lateinit var mockResponse: TestResponse

    private val mockStringResponse = "mockStringResponse"
    private val mockJson = "mockJson"
    private val mockStringResponseByteArray = mockStringResponse.toByteArray()
    private val mockJsonByteArray = mockJson.toByteArray()

    private val mockEncryptedByteArray = "4567".toByteArray()
    private val mockCompressedByteArray = "5678".toByteArray()
    private val mockEncryptedCompressedByteArray = "6789".toByteArray()

    private val mockUncompressedByteArray = "8765".toByteArray()
    private val mockDecryptedByteArray = "7654".toByteArray()
    private val mockDecryptedUncompressedByteArray = "9876".toByteArray()

    private val mockStoredData = "mockStoredData".toByteArray()

    private lateinit var target: SerialisationManager<Glitch>

    private fun setUp(useString: Boolean,
                      isEncryptionSupported: Boolean) {
        mockEncryptionManager = mock()
        mockSerialiser = mock()
        mockCompresser = mock()
        mockUncompresser = mock()
        mockByteToStringConverter = mock()
        mockOnError = mock()
        mockInstructionToken = mock()
        mockInstruction = mock()
        mockResponse = mock()
        mockFileSerialisationDecorator = mock()
        mockCompressionSerialisationDecorator = mock()
        mockEncryptionSerialisationDecorator = mock()

        whenever(mockInstructionToken.instruction).thenReturn(mockInstruction)
        whenever(mockInstruction.responseClass).thenReturn(TestResponse::class.java)
        whenever(mockEncryptionManager.isEncryptionAvailable).thenReturn(isEncryptionSupported)

        mockWrapper = ResponseWrapper(
                if (useString) String::class.java else TestResponse::class.java,
                if (useString) mockStringResponse else mockResponse,
                mock()
        )

        target = SerialisationManager.Factory(
                mock(), //FIXME test serialiser
                mockByteToStringConverter,
                mockFileSerialisationDecorator, //TODO move logic to separate tests
                mockCompressionSerialisationDecorator,
                mockEncryptionSerialisationDecorator
        ).create(FILE) //TODO test factory
    }

    @Test
    fun testSerialise() {
        var iteration = 0
        trueFalseSequence { hasEncryptionManager ->
            trueFalseSequence { encryptionSucceeds ->
                trueFalseSequence { useString ->
                    trueFalseSequence { encryptData ->
                        trueFalseSequence { compressData ->
                            testSerialise(
                                    iteration,
                                    hasEncryptionManager,
                                    encryptionSucceeds && hasEncryptionManager,
                                    useString,
                                    encryptData,
                                    compressData
                            )
                            iteration++
                        }
                    }
                }
            }
        }
    }

    private fun testSerialise(iteration: Int,
                              hasEncryptionManager: Boolean,
                              encryptionSucceeds: Boolean,
                              useString: Boolean,
                              encryptData: Boolean,
                              compressData: Boolean) {
        val context = "iteration = $iteration,\n" +
                "hasEncryptionManager = $hasEncryptionManager,\n" +
                "encryptionSucceeds = $encryptionSucceeds,\n" +
                "useString = $useString,\n" +
                "encryptData = $encryptData,\n" +
                "compressData = $compressData"

        setUp(
                useString,
                hasEncryptionManager
        )

        if (!useString) {
            whenever(mockSerialiser.canHandleType(TestResponse::class.java)).thenReturn(true)
            whenever(mockSerialiser.serialise(mockResponse)).thenReturn(mockJson)
        }

        val expectedInputMockByteArray = if (useString) mockStringResponseByteArray else mockJsonByteArray

        val shouldEncryptData = encryptData && hasEncryptionManager
        if (shouldEncryptData) {
            whenever(mockEncryptionManager.encryptBytes(
                    eq(expectedInputMockByteArray),
                    eq("DATA_TAG"),
                    isNull()
            )).thenReturn(if (encryptionSucceeds) mockEncryptedByteArray else null)
        }

        if (compressData) {
            whenever(mockCompresser.invoke(
                    eq(if (shouldEncryptData) mockEncryptedByteArray else expectedInputMockByteArray)
            )).thenReturn(
                    if (shouldEncryptData) mockEncryptedCompressedByteArray else mockCompressedByteArray
            )
        }

        val serialised = target.serialise(
                mockWrapper,
                SerialisationDecorationMetadata(compressData, encryptData)
        )

        if (useString) {
            val innerContext = "Serialiser should not be called when given a String"
            verifyNeverWithContext(mockSerialiser, innerContext).canHandleType(any())
            verifyNeverWithContext(mockSerialiser, innerContext).serialise<Any>(any())
        }

        val expectedOutput = when {
            shouldEncryptData -> when {
                encryptionSucceeds -> if (compressData) mockEncryptedCompressedByteArray else mockEncryptedByteArray
                else -> null
            }
            compressData -> mockCompressedByteArray
            else -> expectedInputMockByteArray
        }

        assertEqualsWithContext(
                expectedOutput,
                serialised,
                "Output byte array didn't match",
                context
        )
    }

    @Test
    fun testDeserialise() {
        var iteration = 0
        trueFalseSequence { isCompressed ->
            trueFalseSequence { isEncrypted ->
                trueFalseSequence { decryptionSucceeds ->
                    testDeserialise(
                            iteration,
                            isCompressed,
                            isEncrypted,
                            decryptionSucceeds
                    )
                    iteration++
                }
            }
        }
    }

    private fun testDeserialise(iteration: Int,
                                isCompressed: Boolean,
                                isEncrypted: Boolean,
                                decryptionSucceeds: Boolean) {
        val context = "iteration = $iteration,\n" +
                "isCompressed = $isCompressed,\n" +
                "isEncrypted = $isEncrypted,\n" +
                "decryptionSucceeds = $decryptionSucceeds"

        setUp(
                false,
                true
        )

        val mockJsonByteArray = if (isCompressed) {
            whenever(mockUncompresser.invoke(
                    eq(mockStoredData),
                    eq(0),
                    eq(mockStoredData.size)
            )).thenReturn(mockUncompressedByteArray)

            mockUncompressedByteArray
        } else {
            mockStoredData
        }.let { expectedInput ->

            val decrypted = if (isEncrypted) {
                if (decryptionSucceeds) {
                    if (isCompressed) mockDecryptedUncompressedByteArray else mockDecryptedByteArray
                } else null
            } else expectedInput

            if (isEncrypted) {
                whenever(mockEncryptionManager.decryptBytes(
                        eq(expectedInput),
                        eq("DATA_TAG"),
                        isNull()
                )).thenReturn(decrypted)
            }

            decrypted
        }

        if (mockJsonByteArray != null) {
            whenever(
                    mockByteToStringConverter.invoke(eq(mockJsonByteArray))
            ).thenReturn(mockJson)
        }

        whenever(mockSerialiser.deserialise(
                eq(mockJson),
                eq(TestResponse::class.java)
        )).thenReturn(mockResponse)

        val result = target.deserialise(
                mockInstructionToken,
                mockStoredData,
                SerialisationDecorationMetadata(isCompressed, isEncrypted)
        )

        if (mockJsonByteArray == null) {
            verify(mockOnError).invoke()    //TODO check error logic
            assertNullWithContext(
                    result,
                    "Result should be null",
                    context
            )
        } else {
            assertEqualsWithContext(
                    TestResponse::class.java,
                    result.responseClass,
                    "Response class didn't match",
                    context
            )

            assertEqualsWithContext(
                    mockResponse,
                    result.response,
                    "Response didn't match",
                    context
            )

            val metadata = result.metadata

            assertEqualsWithContext(
                    mockInstructionToken,
                    metadata.cacheToken,
                    "Metadata cache token didn't match",
                    context
            )

            assertNullWithContext(
                    metadata.exception,
                    "Metadata exception should be null",
                    context
            )
        }
    }
}