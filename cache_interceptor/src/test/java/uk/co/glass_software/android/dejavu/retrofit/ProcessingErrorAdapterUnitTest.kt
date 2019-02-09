package uk.co.glass_software.android.dejavu.retrofit

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import retrofit2.Call
import retrofit2.CallAdapter
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.ErrorInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.interceptors.internal.response.ResponseInterceptor
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.*
import uk.co.glass_software.android.dejavu.retrofit.annotations.CacheException
import uk.co.glass_software.android.dejavu.test.assertEqualsWithContext
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import java.util.*

@Suppress("UNCHECKED_CAST")
class ProcessingErrorAdapterUnitTest {

    private lateinit var mockDefaultAdapter: CallAdapter<Any, Any>
    private lateinit var mockErrorInterceptorFactory: (CacheToken, Long) -> ErrorInterceptor<Glitch>
    private lateinit var mockResponseInterceptorFactory: (CacheToken, Boolean, Boolean, Long) -> ResponseInterceptor<Glitch>
    private lateinit var mockCacheToken: CacheToken
    private lateinit var mockException: CacheException
    private lateinit var mockErrorInterceptor: ErrorInterceptor<Glitch>
    private lateinit var mockResponseInterceptor: ResponseInterceptor<Glitch>
    private lateinit var mockCall: Call<Any>
    private lateinit var mockMetadata: CacheMetadata<Glitch>
    private lateinit var upstreamCaptor: ArgumentCaptor<Observable<Any>>
    private lateinit var metadataCaptor: ArgumentCaptor<CacheMetadata<Glitch>>
    private lateinit var mockResponseWrapper: ResponseWrapper<Glitch>

    private val mockStart = 1234L

    private lateinit var targetFactory: ProcessingErrorAdapter.Factory<Glitch>

    @Before
    fun setUp() {
        mockDefaultAdapter = mock()
        mockErrorInterceptorFactory = mock()
        mockResponseInterceptorFactory = mock()
        mockCacheToken = mock()
        mockException = mock()
        mockErrorInterceptor = mock()
        mockResponseInterceptor = mock()
        mockCall = mock()

        mockMetadata = CacheMetadata(
                mock(),
                mock(),
                CacheMetadata.Duration(0, 0, 0)
        )

        mockResponseWrapper = ResponseWrapper(
                TestResponse::class.java,
                null,
                mockMetadata
        )

        upstreamCaptor = ArgumentCaptor.forClass(Observable::class.java) as ArgumentCaptor<Observable<Any>>
        metadataCaptor = ArgumentCaptor.forClass(CacheMetadata::class.java) as ArgumentCaptor<CacheMetadata<Glitch>>

        targetFactory = ProcessingErrorAdapter.Factory(
                mockErrorInterceptorFactory,
                mockResponseInterceptorFactory,
                { Date(4321L) }
        )
    }

    private fun createTarget(mockRxType: AnnotationProcessor.RxType): CallAdapter<Any, Any> {
        whenever(mockErrorInterceptorFactory.invoke(
                eq(mockCacheToken),
                eq(mockStart)
        )).thenReturn(mockErrorInterceptor)

        whenever(mockResponseInterceptorFactory.invoke(
                eq(mockCacheToken),
                eq(false),
                eq(false),
                eq(mockStart)
        )).thenReturn(mockResponseInterceptor)

        return targetFactory.create(
                mockDefaultAdapter,
                mockCacheToken,
                1234L,
                mockRxType,
                mockException
        ) as CallAdapter<Any, Any>
    }

    @Test
    fun testAdaptObservable() {
        testAdapt(OBSERVABLE)
    }

    @Test
    fun testAdaptSingle() {
        testAdapt(SINGLE)
    }

    private fun testAdapt(rxType: AnnotationProcessor.RxType) {
        whenever(mockErrorInterceptor.apply(any())).thenReturn(Observable.just(mockResponseWrapper))
        whenever(mockResponseInterceptor.apply(any())).thenReturn(Observable.just(mockResponseWrapper))

        val adapted = createTarget(rxType).adapt(mockCall)

        val wrapper = when (rxType) {
            OBSERVABLE -> (adapted as Observable<Any>).blockingFirst()
            SINGLE -> (adapted as Single<Any>).blockingGet()
            COMPLETABLE -> (adapted as Completable).blockingAwait()
        }

        assertEqualsWithContext(
                mockResponseWrapper,
                wrapper,
                "The adapted call return the wrong response wrapper"
        )
    }

}