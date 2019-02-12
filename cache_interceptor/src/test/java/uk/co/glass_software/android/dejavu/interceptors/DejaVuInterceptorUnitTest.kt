package uk.co.glass_software.android.dejavu.interceptors

import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.CacheInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.Hasher
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.ErrorInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.interceptors.internal.response.ResponseInterceptor
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

    private lateinit var targetFactory: DejaVuInterceptor.Factory<Glitch>

    @Before
    fun setUp() {
        mockHasher = mock()

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
        operationSequence { operation ->
            targetFactory.create(
                    instructionToken(operation).instruction,
                    url
            )
        }
    }

//    @Test
//    fun testApplySingle() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    @Test
//    fun testApplyCompletable() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
}