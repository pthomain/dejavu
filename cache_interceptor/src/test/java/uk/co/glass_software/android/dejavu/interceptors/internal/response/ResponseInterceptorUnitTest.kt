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
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus.*
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.retrofit.annotations.CacheException
import uk.co.glass_software.android.dejavu.test.*
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import java.util.*
import kotlin.NoSuchElementException

class ResponseInterceptorUnitTest {

    private lateinit var mockEmptyResponseFactory: EmptyResponseFactory<Glitch>
    private lateinit var mockConfiguration: CacheConfiguration<Glitch>
    private lateinit var mockMetadataSubject: PublishSubject<CacheMetadata<Glitch>>
    private lateinit var mockEmptyException: Glitch

    private val start = 4321L
    private val mockDateFactory: (Long?) -> Date = { Date(1234L) }

    private var num = 0

    @Before
    fun setUp() {
        mockEmptyResponseFactory = mock()
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
        operationSequence { operation ->
            cacheStatusSequence { cacheStatus ->
                if (isStatusValid(cacheStatus, operation)) {
                    trueFalseSequence { hasResponse ->
                        trueFalseSequence { isEmptyObservable ->
                            trueFalseSequence { allowNonFinalForSingle ->
                                trueFalseSequence { mergeOnNextOnError ->
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
    }

    private fun isStatusValid(cacheStatus: CacheStatus,
                              operation: CacheInstruction.Operation) = when (operation) {
        is Expiring.Cache,
        is Expiring.Refresh -> listOf(
                FRESH,
                CACHED,
                STALE,
                REFRESHED,
                COULD_NOT_REFRESH
        ).contains(cacheStatus)

        is Expiring.Offline -> listOf(
                FRESH,
                STALE,
                EMPTY
        ).contains(cacheStatus)

        CacheInstruction.Operation.DoNotCache -> cacheStatus == NOT_CACHED

        CacheInstruction.Operation.Invalidate,
        is CacheInstruction.Operation.Clear -> cacheStatus == EMPTY
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

        System.out.println(context)
        System.out.println(++num)

        setUp() //reset mocks

        val mockInstructionToken = instructionToken(operation)
        mockEmptyException = Glitch(NoSuchElementException("no response"))

        val isValid = if (operation is Expiring) {
            val filterFresh = cacheStatus.isFresh || !operation.freshOnly
            val filterFinal = cacheStatus.isFinal || !operation.filterFinal

            if (filterFresh && filterFinal) {
                if (isSingle) {
                    cacheStatus.isFinal || (allowNonFinalForSingle && !operation.filterFinal)
                } else true
            } else false
        } else true

        val expectEmpty = !hasResponse
                || isEmptyUpstreamObservable
                || !isValid

        val mockMetadata = CacheMetadata(
                mockInstructionToken.copy(status = if (expectEmpty) EMPTY else cacheStatus),
                if (expectEmpty) mockEmptyException else null
        )

        val mockResponse = mock<TestResponse>()

        val mockWrapper = ResponseWrapper(
                TestResponse::class.java,
                if (hasResponse) mockResponse else null,
                mockMetadata
        )

        val mockUpstreamObservable = if (isEmptyUpstreamObservable)
            Observable.empty<ResponseWrapper<Glitch>>()
        else
            Observable.just(mockWrapper)

        mockConfiguration = CacheConfiguration(
                mock(),
                mock(),
                mock(),
                mock(),
                true,
                true,
                true,
                mergeOnNextOnError,
                allowNonFinalForSingle,
                5,
                5,
                5,
                false
        )

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


        val mockEmptyResponseWrapper = mockWrapper.copy(
                response = null,
                metadata = mockMetadata.copy(
                        cacheToken = mockInstructionToken.copy(status = EMPTY),
                        exception = mockEmptyException
                )
        )

        val expectedMergeOnNextOnError = (operation as? Expiring)?.mergeOnNextOnError
                ?: mergeOnNextOnError

        whenever(mockEmptyResponseFactory.emptyResponseWrapperSingle(
                eq(mockInstructionToken)
        )).thenReturn(Single.just(mockEmptyResponseWrapper))

        val testObserver = TestObserver<Any>()

        target.apply(mockUpstreamObservable).subscribe(testObserver)

        verifyWithContext(
                mockMetadataSubject,
                atLeastOnce(),
                context
        ).onNext(eq(mockMetadata))

        if (isValid) {
            if (expectEmpty) {
                verifyCheckForError(
                        expectedMergeOnNextOnError,
                        testObserver,
                        context
                )
            } else {
                verifyAddMetadataIfPossible(expectedMergeOnNextOnError)
            }
        } else {
            verifyCheckForError(
                    expectedMergeOnNextOnError,
                    testObserver,
                    context
            )
        }
    }

    private fun verifyCheckForError(expectedMergeOnNextOnError: Boolean,
                                    testObserver: TestObserver<Any>,
                                    context: String) {
        if (expectedMergeOnNextOnError) {
            verifyExpectedException(
                    CacheException(
                            CacheException.Type.METADATA,
                            "Could not add cache metadata to response '${TestResponse::class.java.simpleName}'." +
                                    " If you want to enable metadata for this class, it needs extend the" +
                                    " 'CacheMetadata.Holder' interface." +
                                    " The 'mergeOnNextOnError' directive will be cause an exception to be thrown for classes" +
                                    " that do not support cache metadata."
                    ),
                    testObserver,
                    context
            )
        } else {
            verifyExpectedException(
                    mockEmptyException,
                    testObserver,
                    context
            )
        }
    }

    private fun verifyExpectedException(expectedException: Exception,
                                        testObserver: TestObserver<Any>,
                                        context: String) {
        val actualException = testObserver.errors().firstOrNull()

        assertNotNullWithContext(
                actualException,
                "Expected an exception that wasn't thrown",
                context
        )

        assertEqualsWithContext(
                expectedException.javaClass,
                actualException!!.javaClass,
                "Could not find the expected exception on the returned Observable: different type",
                context
        )

        assertEqualsWithContext(
                expectedException.message,
                actualException.message,
                "Could not find the expected exception on the returned Observable: different message",
                context
        )
    }

    private fun verifyAddMetadataIfPossible(expectedMergeOnNextOnError: Boolean) {
        //TODO
    }

}
