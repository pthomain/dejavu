package uk.co.glass_software.android.dejavu.interceptors.internal.response

import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.ErrorFactory
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus.EMPTY
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.test.*
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse

class EmptyResponseFactoryUnitTest {

    private lateinit var mockErrorFactory: ErrorFactory<Glitch>

    private lateinit var target: EmptyResponseFactory<Glitch>

    @Before
    fun setUp() {
        mockErrorFactory = mock()
        target = EmptyResponseFactory(mockErrorFactory)
    }

    @Test
    fun testCreateMergeOnNextOnErrorTrue() {
        assertTrueWithContext(
                target.create(
                        true,
                        TestResponse::class.java
                ) is TestResponse,
                "Factory should return an instance of TestResponse"
        )
    }

    @Test
    fun testCreateMergeOnNextOnErrorFalse() {
        assertNullWithContext(
                target.create(
                        false,
                        TestResponse::class.java
                ),
                "Factory should return null when mergeOnNextOnError is false"
        )
    }

    @Test
    fun testEmptyResponseWrapperObservable() {
        val instructionToken = instructionToken()
        val mockError = mock<Glitch>()

        whenever(mockErrorFactory.getError(any())).thenReturn(mockError)

        val wrapper = target.emptyResponseWrapperObservable(
                instructionToken
        ).blockingFirst()

        val captor = argumentCaptor<NoSuchElementException>()
        verify(mockErrorFactory).getError(captor.capture())
        val capturedException = captor.firstValue

        assertNotNullWithContext(
                capturedException,
                "Wrong exception"
        )

        assertEqualsWithContext(
                TestResponse::class.java,
                wrapper.responseClass,
                "Wrong response class"
        )

        assertNullWithContext(
                wrapper.response,
                "Response should be null"
        )

        val metadata = wrapper.metadata

        assertEqualsWithContext(
                mockError,
                metadata.exception,
                "Exception didn't match"
        )

        assertEqualsWithContext(
                instructionToken.copy(status = EMPTY),
                metadata.cacheToken,
                "Cache token status should be EMPTY"
        )
    }

}