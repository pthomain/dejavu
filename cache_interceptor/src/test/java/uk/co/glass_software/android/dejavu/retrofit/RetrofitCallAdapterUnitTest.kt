package uk.co.glass_software.android.dejavu.retrofit

import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.CallAdapter
import uk.co.glass_software.android.boilerplate.core.utils.log.Logger
import uk.co.glass_software.android.dejavu.DejaVu.Companion.DejaVuHeader
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Cache
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Refresh
import uk.co.glass_software.android.dejavu.configuration.CacheInstructionSerialiser
import uk.co.glass_software.android.dejavu.interceptors.DejaVuInterceptor
import uk.co.glass_software.android.dejavu.interceptors.DejaVuTransformer
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.RequestMetadata
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.retrofit.RetrofitCallAdapterFactory.Companion.DEFAULT_URL
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.*
import uk.co.glass_software.android.dejavu.test.assertEqualsWithContext
import uk.co.glass_software.android.dejavu.test.assertTrueWithContext
import uk.co.glass_software.android.dejavu.test.defaultRequestMetadata
import uk.co.glass_software.android.dejavu.test.instructionToken
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import java.lang.reflect.Type

class RetrofitCallAdapterUnitTest {

    private lateinit var mockDejaVuFactory: DejaVuInterceptor.Factory<Glitch>
    private lateinit var mockLogger: Logger
    private lateinit var mockRxCallAdapter: CallAdapter<Any, Any>
    private lateinit var mockCall: Call<Any>
    private lateinit var mockCacheInstructionSerialiser: CacheInstructionSerialiser
    private lateinit var mockRequest: Request
    private lateinit var mockDejaVuTransformer: DejaVuTransformer
    private lateinit var mockTestResponse: TestResponse
    private lateinit var requestMetadata: RequestMetadata.UnHashed

    private val mockMethodDescription = "mockMethodDescription"
    private val mockHeader = "mockHeader"
    private val mockInstruction = instructionToken(Cache()).instruction
    private val mockHeaderInstruction = instructionToken(Refresh()).instruction

    @Before
    fun setUp() {
        mockDejaVuFactory = mock()
        mockLogger = mock()
        mockRxCallAdapter = mock()
        mockCall = mock()
        mockCacheInstructionSerialiser = mock()
        mockRequest = mock()
        mockDejaVuTransformer = mock()
        mockTestResponse = mock()
        requestMetadata = defaultRequestMetadata()
    }

    private fun getTarget(hasInstruction: Boolean,
                          hasHeader: Boolean,
                          isHeaderDeserialisationSuccess: Boolean,
                          isHeaderDeserialisationException: Boolean): RetrofitCallAdapter<Glitch> {
        whenever(mockCall.request()).thenReturn(mockRequest)

        if (hasHeader) {
            whenever(mockRequest.header(eq(DejaVuHeader))).thenReturn(mockHeader)
            whenever(mockCacheInstructionSerialiser.deserialise(eq(mockHeader))).apply {
                if (isHeaderDeserialisationException)
                    thenThrow(RuntimeException("error"))
                else
                    thenReturn(if (isHeaderDeserialisationSuccess) mockHeaderInstruction else null)
            }
        }

        return RetrofitCallAdapter(
                mockDejaVuFactory,
                mockCacheInstructionSerialiser,
                mockLogger,
                mockMethodDescription,
                if (hasInstruction) mockInstruction else null,
                mockRxCallAdapter
        )
    }

    private fun testAdapt(hasInstruction: Boolean,
                          hasHeader: Boolean,
                          isHeaderDeserialisationSuccess: Boolean,
                          isHeaderDeserialisationException: Boolean) {
        sequenceOf(
                null,
                OBSERVABLE,
                SINGLE,
                COMPLETABLE
        ).forEach { rxType ->
            setUp() //reset mocks

            val target = getTarget(
                    hasInstruction,
                    hasHeader,
                    isHeaderDeserialisationSuccess,
                    isHeaderDeserialisationException
            )

            val rxCall = when (rxType) {
                OBSERVABLE -> Observable.just(mockTestResponse)
                SINGLE -> Single.just(mockTestResponse)
                COMPLETABLE -> Completable.complete()
                else -> mockTestResponse
            }

            whenever(mockRxCallAdapter.adapt(eq(mockCall))).thenReturn(rxCall)

            if (hasInstruction || (hasHeader && isHeaderDeserialisationSuccess)) {
                val mockUrl = mock<HttpUrl>()
                whenever(mockRequest.url()).thenReturn(mockUrl)
                whenever(mockUrl.toString()).thenReturn(DEFAULT_URL)

                val mockBody = mock<RequestBody>()
                whenever(mockRequest.body()).thenReturn(mockBody)
                whenever(mockBody.toString()).thenReturn("body")

                if (rxType != null) {
                    whenever(mockDejaVuFactory.create(
                            eq(if (hasHeader && isHeaderDeserialisationSuccess) mockHeaderInstruction else mockInstruction),
                            eq(requestMetadata)
                    )).thenReturn(mockDejaVuTransformer)

                    when (rxType) {
                        OBSERVABLE -> whenever(mockDejaVuTransformer.apply(rxCall as Observable<Any>)).thenReturn(rxCall)
                        SINGLE -> whenever(mockDejaVuTransformer.apply(rxCall as Single<Any>)).thenReturn(rxCall)
                        COMPLETABLE -> whenever(mockDejaVuTransformer.apply(rxCall as Completable)).thenReturn(rxCall)
                    }
                }
            }

            val actualAdapted = target.adapt(mockCall)

            val context = "For rxType == $rxType"

            if (rxType == null) {
                verify(mockDejaVuFactory, never()).create(
                        any(),
                        any()
                )

                assertEqualsWithContext(
                        mockTestResponse,
                        actualAdapted,
                        "Adapted value should be the mocked TestResponse",
                        context
                )
            } else {
                if (hasInstruction || isHeaderDeserialisationSuccess) {
                    verify(mockDejaVuFactory).create(
                            eq(if (hasHeader && isHeaderDeserialisationSuccess) mockHeaderInstruction else mockInstruction),
                            eq(requestMetadata)
                    )
                }

                when (rxType) {
                    OBSERVABLE -> assertTrueWithContext(
                            Observable::class.java.isAssignableFrom(actualAdapted.javaClass),
                            "Adapted result should be of type Observable",
                            context
                    )

                    SINGLE -> assertTrueWithContext(
                            Single::class.java.isAssignableFrom(actualAdapted.javaClass),
                            "Adapted result should be of type Single",
                            context
                    )

                    COMPLETABLE -> assertTrueWithContext(
                            Completable::class.java.isAssignableFrom(actualAdapted.javaClass),
                            "Adapted result should be of type Completable",
                            context
                    )
                }
            }
        }
    }

    @Test
    fun testResponseType() {
        val mockResponseType = mock<Type>()
        whenever(mockRxCallAdapter.responseType()).thenReturn(mockResponseType)

        assertEqualsWithContext(
                mockResponseType,
                getTarget(
                        false,
                        false,
                        false,
                        false
                ).responseType(),
                "Response type didn't match"
        )
    }

    @Test
    fun testAdaptWithNoInstructionAndNoHeader() {
        val target = getTarget(
                false,
                false,
                false,
                false
        )

        val mockAdapted = mock<Any>()
        whenever(mockRxCallAdapter.adapt(eq(mockCall))).thenReturn(mockAdapted)

        assertEqualsWithContext(
                mockAdapted,
                target.adapt(mockCall),
                "Adapter returned the wrong value"
        )
    }

    @Test
    fun testAdaptWithNoInstructionAndHeader() {
        testAdapt(
                false,
                true,
                true,
                false
        )
    }

    @Test
    fun testAdaptWithNoInstructionAndHeaderDeserialisationReturnsNull() {
        testAdapt(
                false,
                true,
                false,
                false
        )
    }

    @Test
    fun testAdaptWithNoInstructionAndHeaderDeserialisationThrowsException() {
        testAdapt(
                false,
                true,
                false,
                true
        )
    }

    @Test
    fun testAdaptWithInstructionAndNoHeader() {
        testAdapt(
                true,
                false,
                true,
                false
        )
    }

    @Test
    fun testAdaptWithInstructionAndHeaderDeserialisationReturnsNull() {
        testAdapt(
                true,
                true,
                false,
                false
        )
    }

    @Test
    fun testAdaptWithInstructionAndHeaderDeserialisationThrowsException() {
        testAdapt(
                true,
                true,
                false,
                true
        )
    }
}