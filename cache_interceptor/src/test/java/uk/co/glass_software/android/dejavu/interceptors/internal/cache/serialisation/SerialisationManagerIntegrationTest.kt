package uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import uk.co.glass_software.android.boilerplate.utils.lambda.Action
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Cache
import uk.co.glass_software.android.dejavu.injection.integration.component.IntegrationCacheComponent
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.test.BaseIntegrationTest
import uk.co.glass_software.android.dejavu.test.assertResponseWrapperWithContext
import uk.co.glass_software.android.dejavu.test.instructionToken

internal class SerialisationManagerIntegrationTest
    : BaseIntegrationTest<SerialisationManager<Glitch>>(IntegrationCacheComponent::serialisationManager) {

    private lateinit var wrapper: ResponseWrapper<Glitch>
    private lateinit var instructionToken: CacheToken
    private lateinit var mockErrorCallback: Action

    @Before
    @Throws(Exception::class)
    fun setUp() {
        instructionToken = instructionToken(Cache())
        mockErrorCallback = mock()

        wrapper = getStubbedTestResponse(instructionToken)
    }

    @Test
    @Throws(Exception::class)
    fun testCompress() {
        val compressed = target.serialise(
                wrapper,
                false,
                true
        )

        assertEquals(
                "Wrong compressed size",
                2566,
                compressed!!.size
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUncompressSuccess() {
        val compressed = target.serialise(
                wrapper,
                false,
                true
        )!!

        val uncompressed = target.deserialise(
                instructionToken,
                compressed,
                false,
                true,
                mockErrorCallback
        )

        assertResponseWrapperWithContext(
                wrapper,
                uncompressed!!,
                "Response wrapper didn't match"
        )

        verify(mockErrorCallback, never()).invoke()
    }

    @Test
    @Throws(Exception::class)
    fun testUncompressFailure() {
        val compressed = target.serialise(
                wrapper,
                false,
                true
        )!!

        for (i in 0..49) {
            compressed[i] = 0
        }

        target.deserialise(
                instructionToken,
                compressed,
                false,
                true,
                mockErrorCallback
        )

        verify(mockErrorCallback).invoke()
    }
}