package uk.co.glass_software.android.dejavu.retrofit

import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
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
    private lateinit var mockErrorInterceptorFactory: (CacheToken, Long, AnnotationProcessor.RxType) -> ErrorInterceptor<Glitch>
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
                mock()
        )

        mockResponseWrapper = ResponseWrapper(
                TestResponse::class.java,
                null,
                mockMetadata
        )

        upstreamCaptor = ArgumentCaptor.forClass(Observable::class.java) as ArgumentCaptor<Observable<Any>>
        metadataCaptor = ArgumentCaptor.forClass(CacheMetadata::class.java) as ArgumentCaptor<CacheMetadata<Glitch>>

        whenever(mockErrorInterceptor.apply(any())).thenReturn(Observable.just(mockResponseWrapper))
        whenever(mockResponseWrapper.metadata).thenReturn(mockMetadata)

        targetFactory = ProcessingErrorAdapter.Factory(
                mockErrorInterceptorFactory,
                mockResponseInterceptorFactory,
                { Date(4321L) }
        )
    }

    private fun createTarget(mockRxType: AnnotationProcessor.RxType): ProcessingErrorAdapter<Glitch> {
        whenever(mockErrorInterceptorFactory.invoke(
                eq(mockCacheToken),
                eq(mockStart),
                eq(mockRxType)
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
        )
    }

    @Test
    fun testAdaptObservable() {
        testAdapt(OBSERVABLE)
    }

    @Test
    fun testAdaptSingle() {
        testAdapt(SINGLE)
    }

    @Test
    fun testAdaptCompletable() {
        testAdapt(COMPLETABLE)
    }

    private fun testAdapt(rxType: AnnotationProcessor.RxType) {
        val target = createTarget(rxType)
        val errorSubscriber = TestObserver<Any>()

        val adapted = target.adapt(mockCall)

        when(rxType){
            OBSERVABLE -> (adapted as Observable<Any>).subscribe(errorSubscriber)
            SINGLE -> (adapted as Single<Any>).subscribe(errorSubscriber)
            COMPLETABLE -> (adapted as Completable).subscribe(errorSubscriber)
        }

        assertEqualsWithContext(
                0,
                mockMetadata.callDuration.disk,
                "Call duration for disk was wrong"
        )

        assertEqualsWithContext(
                0,
                mockMetadata.callDuration.network,
                "Call duration for network was wrong"
        )

        assertEqualsWithContext(
                4321L - 1234L,
                mockMetadata.callDuration.total,
                "Call duration for disk was wrong"
        )

        assertEqualsWithContext(
                1,
                errorSubscriber.errorCount(),
                "Observable should emit one exception"
        )
    }

}