package uk.co.glass_software.android.dejavu.interceptors

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.CacheInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.Hasher
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.RequestMetadata
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.ErrorInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.interceptors.internal.response.ResponseInterceptor
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.*
import uk.co.glass_software.android.dejavu.test.defaultRequestMetadata
import uk.co.glass_software.android.dejavu.test.instructionToken
import uk.co.glass_software.android.dejavu.test.operationSequence
import java.util.*

class DejaVuInterceptorUnitTest {

    private val start = 1234L
    private val mockDateFactory: (Long?) -> Date = { Date(4321L) }

    private val url = "http://test.com?param1=value1&param2=value2"

    private lateinit var mockErrorInterceptorFactory: (CacheToken, Long) -> ErrorInterceptor<Glitch>
    private lateinit var mockCacheInterceptorFactory: (CacheToken, Long) -> CacheInterceptor<Glitch>
    private lateinit var mockResponseInterceptorFactory: (CacheToken, Boolean, Boolean, Long) -> ResponseInterceptor<Glitch>
    private lateinit var mockConfiguration: CacheConfiguration<Glitch>
    private lateinit var mockHasher: Hasher
    private lateinit var metadata: RequestMetadata.UnHashed

    private lateinit var targetFactory: DejaVuInterceptor.Factory<Glitch>

    @Before
    fun setUp() {
        mockErrorInterceptorFactory = mock()
        mockCacheInterceptorFactory = mock()
        mockResponseInterceptorFactory = mock()
        mockConfiguration = mock()
        mockHasher = mock()

        metadata = defaultRequestMetadata()

        whenever(mockHasher.hash(eq(metadata))).thenReturn(mock())

        targetFactory = DejaVuInterceptor.Factory(mockHasher,
                mockDateFactory,
                mockErrorInterceptorFactory,
                mockCacheInterceptorFactory,
                mockResponseInterceptorFactory,
                mockConfiguration
        )
    }

    @Test
    fun testApplyObservable() {
        testApply(OBSERVABLE)
    }

    @Test
    fun testApplySingle() {
        testApply(SINGLE)
    }

    @Test
    fun testApplyCompletable() {
        testApply(COMPLETABLE)
    }

    private fun testApply(rxType: AnnotationProcessor.RxType) {

        operationSequence { operation ->
            val instructionToken = instructionToken(operation)

            val target = targetFactory.create(
                    instructionToken.instruction,
                    metadata
            )

        }

    }
}