package uk.co.glass_software.android.dejavu.interceptors.internal.response

import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.test.instructionToken
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import uk.co.glass_software.android.dejavu.test.operationSequence
import uk.co.glass_software.android.dejavu.test.verifyWithContext
import java.util.*
import kotlin.NoSuchElementException

class ResponseInterceptorUnitTest {

    //TODO finish

    private lateinit var mockEmptyResponseFactory: EmptyResponseFactory<Glitch>
    private lateinit var mockConfiguration: CacheConfiguration<Glitch>
    private lateinit var mockMetadataSubject: PublishSubject<CacheMetadata<Glitch>>

    private val start = 4321L
    private val mockDateFactory: (Long?) -> Date = { Date(1234L) }

    @Before
    fun setUp() {
        mockEmptyResponseFactory = mock()
        mockConfiguration = mock()
        mockMetadataSubject = mock()
    }

    @Test
    fun testApplyObservable() {
        testApply(false, false)
    }

    @Test
    fun testApplySingle() {
        testApply(true, false)
    }

    @Test
    fun testApplyCompletable() {
        testApply(false, true)
    }

    private fun testApply(isSingle: Boolean,
                          isCompletable: Boolean) {
        operationSequence().forEach { operation ->
            CacheStatus.values().forEach { cacheStatus ->
                sequenceOf(true, false).forEach { hasResponse ->
                    sequenceOf(true, false).forEach { isEmptyObservable ->
                        sequenceOf(true, false).forEach { allowNonFinalForSingle ->
                            sequenceOf(true, false).forEach { mergeOnNextOnError ->
                                testApplyWithVariants(
                                        isSingle,
                                        isCompletable,
                                        hasResponse,
                                        isEmptyObservable,
                                        allowNonFinalForSingle,
                                        mergeOnNextOnError,
                                        cacheStatus,
                                        operation
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun testApplyWithVariants(isSingle: Boolean,
                                      isCompletable: Boolean,
                                      hasResponse: Boolean,
                                      isEmptyUpstreamObservable: Boolean,
                                      allowNonFinalForSingle: Boolean,
                                      mergeOnNextOnError: Boolean,
                                      cacheStatus: CacheStatus,
                                      operation: CacheInstruction.Operation) {
        val context = "\nOperation = ${operation.type}," +
                "\nCacheStatus = $cacheStatus," +
                "\nisSingle = $isSingle," +
                "\nisCompletable = $isCompletable," +
                "\nhasResponse = $hasResponse," +
                "\nisEmptyUpstreamObservable = $isEmptyUpstreamObservable," +
                "\nallowNonFinalForSingle = $allowNonFinalForSingle," +
                "\noperation.mergeOnNextOnError = ${(operation as? Expiring)?.mergeOnNextOnError}," +
                "\nconf.mergeOnNextOnError = $mergeOnNextOnError"

//        System.out.println(context)

        setUp() //reset mocks

        val mockInstructionToken = instructionToken(operation)
        val mockEmptyException = Glitch(NoSuchElementException("no response"))

        val mockMetadata = CacheMetadata(
                mockInstructionToken.copy(status = cacheStatus),
                if (hasResponse) null else mockEmptyException
        )

        val mockResponse = mock<TestResponse>()

        val mockWrapper = ResponseWrapper(
                TestResponse::class.java,
                if (hasResponse) mockResponse else null,
                mockMetadata
        )

        val mockUpstreamObservable = if (false && isEmptyUpstreamObservable) //FIXME
            Observable.empty<ResponseWrapper<Glitch>>()
        else
            Observable.just(mockWrapper)

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

        val mockEmptyResponseWrapper = mockWrapper.copy(
                response = null,
                metadata = mockMetadata.copy(
                        cacheToken = mockInstructionToken.copy(status = CacheStatus.EMPTY),
                        exception = mockEmptyException
                )
        )

        val expectedMergeOnNextOnError = (operation as? Expiring)?.mergeOnNextOnError ?: mergeOnNextOnError

        whenever(mockEmptyResponseFactory.emptyResponseWrapperSingle(
                eq(mockInstructionToken)
        )).thenReturn(Single.just(mockEmptyResponseWrapper))

        val testObserver = TestObserver<Any>()

        target.apply(mockUpstreamObservable).subscribe(testObserver)

        if (isValid) {
            verifyWithContext(
                    mockMetadataSubject,
                    atLeastOnce(),
                    context
            ).onNext(eq(mockMetadata))
//        } else {
//            verifyWithContext(
//                    mockMetadataSubject,
//                    never(),
//                    any()
//            ).onNext(any())
        }

    }

}
