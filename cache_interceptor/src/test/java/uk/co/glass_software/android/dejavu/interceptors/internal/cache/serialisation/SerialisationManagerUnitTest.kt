package uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.test.assertArrayEqualsWithContext
import uk.co.glass_software.android.dejavu.test.assertEqualsWithContext
import uk.co.glass_software.android.dejavu.test.assertNullWithContext
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import uk.co.glass_software.android.shared_preferences.encryption.manager.EncryptionManager

class SerialisationManagerUnitTest {

    private lateinit var mockEncryptionManager: EncryptionManager
    private lateinit var mockWrapper: ResponseWrapper<Glitch>
    private lateinit var mockGson: Gson
    private lateinit var mockCompresser: Function1<ByteArray, ByteArray>
    private lateinit var mockUncompresser: Function3<ByteArray, Int, Int, ByteArray>
    private lateinit var mockByteToStringConverter: (ByteArray) -> String
    private lateinit var mockOnError: () -> Unit
    private lateinit var mockInstructionToken: CacheToken
    private lateinit var mockInstruction: CacheInstruction
    private lateinit var mockResponse: TestResponse

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

    @Before
    fun setUp() {
        mockEncryptionManager = mock()
        mockGson = mock()
        mockCompresser = mock()
        mockUncompresser = mock()
        mockByteToStringConverter = mock()
        mockOnError = mock()
        mockInstructionToken = mock()
        mockInstruction = mock()
        mockResponse = mock()

        mockWrapper = ResponseWrapper(
                TestResponse::class.java,
                mockResponse,
                mock()
        )

        whenever(mockInstructionToken.instruction).thenReturn(mockInstruction)
        whenever(mockInstruction.responseClass).thenReturn(TestResponse::class.java)
    }

    @Test
    fun testSerialiseEncryptFalseCompressFalse() {
        testSerialise(
                false,
                false,
                false,
                false
        )
    }

    @Test
    fun testSerialiseEncryptTrueEncryptionSuccessTrueCompressFalse() {
        testSerialise(
                true,
                true,
                true,
                false
        )
    }

    @Test
    fun testSerialiseEncryptTrueEncryptionSuccessFalseCompressFalse() {
        testSerialise(
                true,
                false,
                true,
                false
        )
    }

    @Test
    fun testSerialiseEncryptFalseCompressTrue() {
        testSerialise(
                false,
                false,
                false,
                true
        )
    }

    @Test
    fun testSerialiseEncryptTrueEncryptionSuccessTrueCompressTrue() {
        testSerialise(
                true,
                true,
                true,
                true
        )
    }

    @Test
    fun testSerialiseEncryptTrueEncryptionSuccessFalseCompressTrue() {
        testSerialise(
                true,
                false,
                true,
                true
        )
    }

    private fun prepareTarget(hasEncryptionManager: Boolean) {
        target = SerialisationManager(
                mock(),
                mockByteToStringConverter,
                if (hasEncryptionManager) mockEncryptionManager else null,
                mockCompresser,
                mockUncompresser,
                mockGson
        )
    }

    private fun testSerialise(hasEncryptionManager: Boolean,
                              encryptionSucceeds: Boolean,
                              encryptData: Boolean,
                              compressData: Boolean) {
        prepareTarget(hasEncryptionManager)

        whenever(mockGson.toJson(mockResponse)).thenReturn(mockJson)

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

        val expectedOutput = when {
            encryptData -> when {
                encryptionSucceeds -> if (compressData) mockEncryptedCompressedByteArray else mockEncryptedByteArray
                else -> null
            }
            compressData -> mockCompressedByteArray
            else -> mockJsonByteArray
        }

        assertArrayEqualsWithContext(
                expectedOutput,
                serialised,
                "Output byte array didn't match"
        )
    }

    @Test
    fun testDeserialiseIsEncryptedFalseIsCompressedFalse() {
        testDeserialise(
                false,
                false,
                false
        )
    }

    @Test
    fun testDeserialiseIsEncryptedFalseIsCompressedTrue() {
        testDeserialise(
                true,
                false,
                false
        )
    }

    @Test
    fun testDeserialiseIsEncryptedTrueDecryptionSuccessFalseIsCompressedTrue() {
        testDeserialise(
                true,
                true,
                false
        )
    }

    @Test
    fun testDeserialiseIsEncryptedTrueDecryptionSuccessTrueIsCompressedTrue() {
        testDeserialise(
                true,
                true,
                true
        )
    }

    @Test
    fun testDeserialiseIsEncryptedTrueDecryptionSuccessFalseIsCompressedFalse() {
        testDeserialise(
                false,
                true,
                false
        )
    }

    @Test
    fun testDeserialiseIsEncryptedTrueDecryptionSuccessTrueIsCompressedFalse() {
        testDeserialise(
                false,
                true,
                true
        )
    }

    private fun testDeserialise(isCompressed: Boolean,
                                isEncrypted: Boolean,
                                decryptionSucceeds: Boolean) {
        prepareTarget(true)

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

            val decrypted = if (isEncrypted && decryptionSucceeds) {
                if (isCompressed) mockDecryptedUncompressedByteArray else mockDecryptedByteArray
            } else expectedInput

            if (isEncrypted) {
                whenever(mockEncryptionManager.decryptBytes(
                        eq(expectedInput),
                        eq("DATA_TAG")
                )).thenReturn(decrypted)
            }

            decrypted
        }

        whenever(
                mockByteToStringConverter.invoke(eq(mockJsonByteArray))
        ).thenReturn(mockJson)

        whenever(mockGson.fromJson(
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
                    "Result should be null"
            )
        } else {
            assertEqualsWithContext(
                    TestResponse::class.java,
                    result!!.responseClass,
                    "Response class didn't match"
            )

            assertEqualsWithContext(
                    mockResponse,
                    result.response,
                    "Response didn't match"
            )

            val metadata = result.metadata

            assertEqualsWithContext(
                    mockInstructionToken,
                    metadata.cacheToken,
                    "Metadata cache token didn't match"
            )

            assertNullWithContext(
                    metadata.exception,
                    "Metadata exception should be null"
            )
        }
    }
}