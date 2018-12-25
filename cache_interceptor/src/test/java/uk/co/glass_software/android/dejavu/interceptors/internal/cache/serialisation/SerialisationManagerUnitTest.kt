package uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.test.assertArrayEqualsWithContext
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import uk.co.glass_software.android.shared_preferences.encryption.manager.EncryptionManager

class SerialisationManagerUnitTest {

    private lateinit var mockEncryptionManager: EncryptionManager
    private lateinit var mockWrapper: ResponseWrapper<Glitch>
    private lateinit var mockGson: Gson
    private lateinit var mockCompresser: Function1<ByteArray, ByteArray>
    private lateinit var mockUncompresser: Function3<ByteArray, Int, Int, ByteArray>
    private lateinit var mockByteToStringConverter: (ByteArray) -> String

    private val mockJson = "mockJson"
    private val mockJsonByteArray = mockJson.toByteArray()
    private val mockEncryptedByteArray = "4567".toByteArray()
    private val mockCompressedByteArray = "5678".toByteArray()
    private val mockEncryptedCompressedByteArray = "6789".toByteArray()
    private val mockResponse = "mockResponse"

    private lateinit var target: SerialisationManager<Glitch>

    @Before
    fun setUp() {
        mockEncryptionManager = mock()
        mockGson = mock()
        mockCompresser = mock()
        mockUncompresser = mock()
        mockByteToStringConverter = mock()

        mockWrapper = ResponseWrapper(
                TestResponse::class.java,
                mockResponse,
                mock()
        )

        whenever(mockGson.toJson(mockResponse)).thenReturn(mockJson)
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

    private fun testSerialise(hasEncryptionManager: Boolean,
                              encryptionSucceeds: Boolean,
                              encryptData: Boolean,
                              compressData: Boolean) {
        prepareTarget(
                hasEncryptionManager,
                encryptData,
                encryptionSucceeds,
                compressData
        )

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

    private fun prepareTarget(hasEncryptionManager: Boolean,
                              encryptData: Boolean,
                              encryptionSucceeds: Boolean,
                              compressData: Boolean) {
        target = SerialisationManager(
                mock(),
                mockByteToStringConverter,
                if (hasEncryptionManager) mockEncryptionManager else null,
                mockCompresser,
                mockUncompresser,
                mockGson
        )

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
    }
}