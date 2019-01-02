package uk.co.glass_software.android.dejavu.interceptors.internal.response

import com.nhaarman.mockitokotlin2.*
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.*
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.*
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus
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

    private val start = 4321L
    private val mergeOnNextOnError = true
    private val mockDateFactory: (Long?) -> Date = { Date(1234L) }

    @Before
    fun setUp() {
        mockEmptyResponseFactory = mock()
        mockConfiguration = mock()
        mockMetadataSubject = mock()
    }

    @Test
    fun testApplyObservable() {
        testApply(
                false,
                false
        )
    }

    @Test
    fun testApplySingle() {
        testApply(
                true,
                false
        )
    }

    @Test
    fun testApplyCompletable() {
        testApply(
                false,
                true
        )
    }

    private fun testApply(isSingle: Boolean,
                          isCompletable: Boolean) {
        sequenceOf(
                DoNotCache,
                Invalidate,
                Clear(),
                Offline(true),
                Offline(false),
                Refresh(freshOnly = true, filterFinal = true),
                Refresh(freshOnly = true, filterFinal = false),
                Refresh(freshOnly = false, filterFinal = true),
                Refresh(freshOnly = false, filterFinal = false),
                Cache(freshOnly = true, filterFinal = true),
                Cache(freshOnly = true, filterFinal = false),
                Cache(freshOnly = false, filterFinal = true),
                Cache(freshOnly = false, filterFinal = false)
        ).forEach { operation ->
            CacheStatus.values().forEach { cacheStatus ->
                sequenceOf(true, false).forEach { allowNonFinalForSingle ->
                    testApplyForOperationAndCacheStatus(
                            isSingle,
                            isCompletable,
                            allowNonFinalForSingle,
                            cacheStatus,
                            operation
                    )
                }
            }
        }
    }

    private fun testApplyForOperationAndCacheStatus(isSingle: Boolean,
                                                    isCompletable: Boolean,
                                                    allowNonFinalForSingle: Boolean,
                                                    cacheStatus: CacheStatus,
                                                    operation: CacheInstruction.Operation) {
        val context = "Operation = ${operation.type}," +
                " CacheStatus = $cacheStatus," +
                " isSingle = $isSingle," +
                " isCompletable = $isCompletable," +
                " allowNonFinalForSingle = $allowNonFinalForSingle"

        setUp() //reset mocks

        val mockInstructionToken = instructionToken(operation)

        val mockMetadata = mock<CacheMetadata<Glitch>>()
        whenever(mockMetadata.cacheToken).thenReturn(mockInstructionToken.copy(status = cacheStatus))

        val mockWrapper = ResponseWrapper<Glitch>(
                TestResponse::class.java,
                mockMetadata,
                CacheMetadata(mockInstructionToken)
        )

        val mockObservable = Observable.just(mockWrapper)

        whenever(mockConfiguration.allowNonFinalForSingle).thenReturn(allowNonFinalForSingle)

        val target = ResponseInterceptor(
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

        val isValid = if (operation is Expiring) when {
            isSingle -> cacheStatus.isFinal || (allowNonFinalForSingle && !operation.filterFinal)
            operation.filterFinal -> cacheStatus.isFinal
            operation.freshOnly -> cacheStatus.isFresh
            else -> true
        } else true

        val mockEmptyResponseWrapper = mock<ResponseWrapper<Glitch>>()

        if (!isValid) {
            whenever(mockEmptyResponseFactory.emptyResponseWrapperObservable(
                    eq(mockInstructionToken)
            )).thenReturn(Observable.just(mockEmptyResponseWrapper))
        }

        val result = target.apply(mockObservable)

        if (isValid) {
            verify(mockEmptyResponseFactory, never()).emptyResponseWrapperObservable(any())
        }
    }

}
