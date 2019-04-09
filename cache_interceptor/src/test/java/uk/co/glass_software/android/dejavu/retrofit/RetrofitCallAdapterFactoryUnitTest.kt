package uk.co.glass_software.android.dejavu.retrofit

import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.boilerplate.core.utils.log.Logger
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.DoNotCache
import uk.co.glass_software.android.dejavu.interceptors.DejaVuInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.retrofit.RetrofitCallAdapterFactory.Companion.DEFAULT_URL
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.*
import uk.co.glass_software.android.dejavu.retrofit.annotations.CacheException
import uk.co.glass_software.android.dejavu.test.assertEqualsWithContext
import uk.co.glass_software.android.dejavu.test.assertFalseWithContext
import uk.co.glass_software.android.dejavu.test.callAdapterFactory
import uk.co.glass_software.android.dejavu.test.instructionToken
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import java.lang.reflect.Type
import java.util.*

class RetrofitCallAdapterFactoryUnitTest {

    private lateinit var mockRxJava2CallAdapterFactory: RxJava2CallAdapterFactory
    private lateinit var mockDejaVuFactory: DejaVuInterceptor.Factory<Glitch>
    private lateinit var mockAnnotationProcessor: AnnotationProcessor<Glitch>
    private lateinit var mockProcessingErrorAdapterFactory: ProcessingErrorAdapter.Factory<Glitch>
    private lateinit var mockDefaultCallAdapter: CallAdapter<Any, Any>
    private lateinit var mockAnnotations: Array<Annotation>
    private lateinit var mockRetrofit: Retrofit
    private lateinit var mockReturnType: Type
    private lateinit var mockCacheInstruction: CacheInstruction
    private lateinit var mockException: CacheException
    private lateinit var mockInnerFactory: Function5<DejaVuInterceptor.Factory<Glitch>, Logger, String, CacheInstruction?, CallAdapter<Any, Any>, CallAdapter<*, *>>
    private lateinit var mockReturnedAdapter: CallAdapter<*, *>

    private val mockDateFactory: (Long?) -> Date = { Date(1234L) }

    private lateinit var targetFactory: RetrofitCallAdapterFactory<Glitch>

    @Before
    fun setUp() {
        mockRxJava2CallAdapterFactory = mock()
        mockDejaVuFactory = mock()
        mockAnnotationProcessor = mock()
        mockProcessingErrorAdapterFactory = mock()
        mockDefaultCallAdapter = mock()
        mockRetrofit = mock()
        mockCacheInstruction = instructionToken().instruction
        mockException = mock()
        mockInnerFactory = mock()
        mockReturnedAdapter = mock()

        targetFactory = RetrofitCallAdapterFactory(
                mockRxJava2CallAdapterFactory,
                mockInnerFactory,
                mockDateFactory,
                mockDejaVuFactory,
                mockAnnotationProcessor,
                mockProcessingErrorAdapterFactory,
                mock()
        )
    }


    private fun testFactory(rxType: AnnotationProcessor.RxType,
                            throwAnnotationException: Boolean = false) {
        callAdapterFactory(rxType.rxClass, mockRetrofit) { returnType, annotations, _ ->
            mockReturnType = returnType
            mockAnnotations = annotations

            whenever(mockRxJava2CallAdapterFactory.get(
                    eq(returnType),
                    eq(mockAnnotations),
                    eq(mockRetrofit)
            )).thenReturn(mockDefaultCallAdapter)

            mockDefaultCallAdapter
        }

        val responseClass = if (rxType == COMPLETABLE) Any::class.java
        else TestResponse::class.java

        whenever(mockAnnotationProcessor.process(
                eq(mockAnnotations),
                eq(rxType),
                eq(responseClass)
        )).also {
            if (throwAnnotationException)
                it.thenThrow(mockException)
            else
                it.thenReturn(mockCacheInstruction)
        }

        val cacheTokenCaptor = argumentCaptor<CacheToken>()

        if (throwAnnotationException) {
            whenever(mockProcessingErrorAdapterFactory.create(
                    eq(mockDefaultCallAdapter),
                    cacheTokenCaptor.capture(),
                    eq(mockDateFactory(null).time),
                    eq(rxType),
                    eq(mockException)
            )).thenReturn(mockReturnedAdapter)
        } else {
            whenever(mockInnerFactory.invoke(
                    eq(mockDejaVuFactory),
                    any(),
                    eq("method returning " + rxType.getTypedName(responseClass)),
                    eq(mockCacheInstruction),
                    eq(mockDefaultCallAdapter)
            )).thenReturn(mockReturnedAdapter)
        }

        assertEqualsWithContext(
                mockReturnedAdapter,
                targetFactory.get(
                        mockReturnType,
                        mockAnnotations,
                        mockRetrofit
                ),
                "Factory didn't return the right adapter"
        )

        if (throwAnnotationException) {
            verify(mockInnerFactory, never()).invoke(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
            )

            val token = cacheTokenCaptor.firstValue

            assertEqualsWithContext(
                    CacheInstruction(responseClass, DoNotCache),
                    token.instruction,
                    "Exception cache token instruction didn't match"
            )

            assertFalseWithContext(
                    token.isCompressed,
                    "Token isCompressed should be false"
            )

            assertFalseWithContext(
                    token.isEncrypted,
                    "Token isEncrypted should be false"
            )

            assertEqualsWithContext(
                    DEFAULT_URL,
                    token.requestMetadata.url,
                    "Exception cache token URL should be empty"
            )
        } else {
            verify(mockProcessingErrorAdapterFactory, never()).create(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
            )
        }
    }

    @Test
    fun testGetObservable() {
        testFactory(OBSERVABLE)
    }

    @Test
    fun testGetSingle() {
        testFactory(SINGLE)
    }

    @Test
    fun testGetCompletable() {
        testFactory(COMPLETABLE)
    }

    @Test
    fun testGetObservableWithAnnotationException() {
        testFactory(OBSERVABLE, true)
    }

    @Test
    fun testGetSingleWithAnnotationException() {
        testFactory(SINGLE, true)
    }

    @Test
    fun testGetCompletableWithAnnotationException() {
        testFactory(COMPLETABLE, true)
    }

}