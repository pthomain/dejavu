package uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation

import com.nhaarman.mockitokotlin2.*
import org.junit.Test
import uk.co.glass_software.android.boilerplate.utils.lambda.Action
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.test.assertEqualsWithContext
import uk.co.glass_software.android.dejavu.test.assertNullWithContext
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import uk.co.glass_software.android.dejavu.test.trueFalseSequence
import uk.co.glass_software.android.dejavu.test.verifyNeverWithContext
import uk.co.glass_software.android.shared_preferences.encryption.manager.EncryptionManager
import uk.co.glass_software.android.shared_preferences.persistence.serialisation.Serialiser

class SerialisationManagerUnitTest {

    private lateinit var mockEncryptionManager: EncryptionManager
    private lateinit var mockWrapper: ResponseWrapper<Glitch>
    private lateinit var mockSerialiser: Serialiser
    private lateinit var mockCompresser: Function1<ByteArray, ByteArray>
    private lateinit var mockUncompresser: Function3<ByteArray, Int, Int, ByteArray>
    private lateinit var mockByteToStringConverter: (ByteArray) -> String
    private lateinit var mockOnError: Action
    private lateinit var mockInstructionToken: CacheToken
    private lateinit var mockInstruction: CacheInstruction
    private lateinit var mockResponse: TestResponse

    private val mockStringResponse = "mockStringResponse"
    private val mockJson = "mockJson"
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
                      hasEncryptionManager: Boolean) {
        mockEncryptionManager = mock()
        mockSerialiser = mock()
        mockCompresser = mock()
        mockUncompresser = mock()
        mockByteToStringConverter = mock()
        mockOnError = mock()
        mockInstructionToken = mock()
        mockInstruction = mock()
        mockResponse = mock()

        whenever(mockInstructionToken.instruction).thenReturn(mockInstruction)
        whenever(mockInstruction.responseClass).thenReturn(TestResponse::class.java)
        mockWrapper = ResponseWrapper(
                if (useString) String::class.java else TestResponse::class.java,
                if (useString) mockStringResponse else mockResponse,
                mock()
        )

        target = SerialisationManager(
                mock(),
                mockByteToStringConverter,
                if (hasEncryptionManager) mockEncryptionManager else null,
                mockCompresser,
                mockUncompresser,
                mockSerialiser
        )
    }

    @Test
    fun testSerialise() {
        trueFalseSequence { hasEncryptionManager ->
            trueFalseSequence { encryptionSucceeds ->
                trueFalseSequence { useString ->
                    trueFalseSequence { encryptData ->
                        trueFalseSequence { compressData ->
                            testSerialise(
                                    hasEncryptionManager,
                                    encryptionSucceeds,
                                    useString,
                                    encryptData,
                                    compressData
                            )
                        }
                    }
                }
            }
        }
    }

    private fun testSerialise(hasEncryptionManager: Boolean,
                              encryptionSucceeds: Boolean,
                              useString: Boolean,
                              encryptData: Boolean,
                              compressData: Boolean) {
        val context = "hasEncryptionManager = $hasEncryptionManager,\n" +
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

        if (encryptData) {
            whenever(mockEncryptionManager.encryptBytes(
                    eq(mockJsonByteArray),
                    eq("DATA_TAG")
            )).thenReturn(if (encryptionSucceeds) mockEncryptedByteArray else null)
        }

        if (compressData) {
            whenever(mockCompresser.invoke(
                    eq(if (encryptData) mockEncryptedByteArray else mockJsonByteArray)
            )).thenReturn(
                    if (encryptData) mockEncryptedCompressedByteArray else mockCompressedByteArray
            )
        }

        val serialised = target.serialise(
                mockWrapper,
                encryptData,
                compressData
        )

        if (useString) {
            val innerContext = "Serialiser should not be called when given a String"
            verifyNeverWithContext(mockSerialiser, innerContext).canHandleType(any())
            verifyNeverWithContext(mockSerialiser, innerContext).serialise<Any>(any())
        }

        val expectedOutput = when {
            encryptData -> when {
                encryptionSucceeds -> if (compressData) mockEncryptedCompressedByteArray else mockEncryptedByteArray
                else -> null
            }
            compressData -> mockCompressedByteArray
            else -> mockJsonByteArray
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
        trueFalseSequence { isCompressed ->
            trueFalseSequence { isEncrypted ->
                trueFalseSequence { decryptionSucceeds ->
                    testDeserialise(
                            isCompressed,
                            isEncrypted,
                            decryptionSucceeds
                    )
                }
            }
        }
    }

    private fun testDeserialise(isCompressed: Boolean,
                                isEncrypted: Boolean,
                                decryptionSucceeds: Boolean) {
        val context = "isCompressed = $isCompressed,\n" +
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
                        eq("DATA_TAG")
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
                isEncrypted,
                isCompressed,
                mockOnError
        )

        if (mockJsonByteArray == null) {
            verify(mockOnError).invoke()
            assertNullWithContext(
                    result,
                    "Result should be null",
                    context
            )
        } else {
            assertEqualsWithContext(
                    TestResponse::class.java,
                    result!!.responseClass,
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