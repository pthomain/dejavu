package uk.co.glass_software.android.dejavu.interceptors.internal.response

import com.nhaarman.mockitokotlin2.mock
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.test.instructionToken
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import java.util.*

class ResponseInterceptorUnitTest {

    //TODO finish

    private lateinit var mockEmptyResponseFactory: EmptyResponseFactory<Glitch>
    private lateinit var mockConfiguration: CacheConfiguration<Glitch>
    private lateinit var mockMetadataSubject: PublishSubject<CacheMetadata<Glitch>>
    private lateinit var mockInstructionToken: CacheToken
    private lateinit var mockWrapper: ResponseWrapper<Glitch>
    private lateinit var mockObservable: Observable<ResponseWrapper<Glitch>>

    private val start = 4321L
    private val mergeOnNextOnError = true
    private val mockDateFactory: (Long?) -> Date = { Date(1234L) }

    @Before
    fun setUp() {
        mockEmptyResponseFactory = mock()
        mockConfiguration = mock()
        mockMetadataSubject = mock()
        mockInstructionToken = mock()

        mockInstructionToken = instructionToken()
        mockWrapper = ResponseWrapper(
                TestResponse::class.java,
                mock(),
                CacheMetadata(
                        mockInstructionToken
                )
        )
        mockObservable = Observable.just(mockWrapper)
    }

    private fun createTarget(isSingle: Boolean,
                             isCompletable: Boolean) =
            ResponseInterceptor(
                    mock(),
                    mockDateFactory,
                    mockEmptyResponseFactory,
                    mockConfiguration,
                    mockMetadataSubject,
                    mockInstructionToken,
                    isSingle,
                    isCompletable,
                    start,
                    mergeOnNextOnError
            )

    @Test
    fun testApplyObservable() {
        val target = createTarget(false, false)

        target.apply(mockObservable)
    }

    @Test
    fun testApplySingle() {
        val target = createTarget(true, false)

    }

    @Test
    fun testApplyCompletable() {
        val target = createTarget(false, true)

    }

}
