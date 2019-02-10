package uk.co.glass_software.android.dejavu.interceptors.internal.cache

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Observable
import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.test.assertEqualsWithContext
import uk.co.glass_software.android.dejavu.test.instructionToken
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import uk.co.glass_software.android.dejavu.test.operationSequence
import java.util.*

class CacheInterceptorUnitTest {

    private lateinit var mockInstructionToken: CacheToken
    private lateinit var mockMetadata: CacheMetadata<Glitch>
    private lateinit var mockUpstream: Observable<ResponseWrapper<Glitch>>
    private lateinit var mockUpstreamResponseWrapper: ResponseWrapper<Glitch>
    private lateinit var mockReturnedResponseWrapper: ResponseWrapper<Glitch>
    private lateinit var mockReturnedObservable: Observable<ResponseWrapper<Glitch>>

    private val mockStart = 1234L
    private val mockDateFactory: (Long?) -> Date = { Date(1234L) }

    private lateinit var mockCacheManager: CacheManager<Glitch>


    private fun getTarget(isCacheEnabled: Boolean,
                          operation: Operation): CacheInterceptor<Glitch> {
        mockCacheManager = mock()

        mockInstructionToken = instructionToken(operation)
        mockMetadata = CacheMetadata(mockInstructionToken)

        mockUpstreamResponseWrapper = ResponseWrapper(
                TestResponse::class.java,
                mock<TestResponse>(),
                mockMetadata
        )
        mockUpstream = Observable.just(mockUpstreamResponseWrapper)

        mockReturnedResponseWrapper = mock()
        mockReturnedObservable = Observable.just(mockReturnedResponseWrapper)

        return CacheInterceptor(
                mockCacheManager,
                mockDateFactory,
                isCacheEnabled,
                mock(),
                mockInstructionToken,
                mockStart
        )
    }

    @Test
    fun testApplyCacheEnabledFalse() {
        testApply(false)
    }

    @Test
    fun testApplyCacheEnabledTrue() {
        testApply(true)
    }

    private fun testApply(isCacheEnabled: Boolean) {
        operationSequence { operation ->
            val target = getTarget(
                    isCacheEnabled,
                    operation
            )

            if (isCacheEnabled) {
                when (operation) {
                    is Expiring -> prepareGetCachedResponse(operation)
                    is Operation.Clear -> prepareClearCache(operation)
                    is Operation.Invalidate -> prepareInvalidate()
                }
            }

            val responseWrapper = target.apply(mockUpstream).blockingFirst()

            if (isCacheEnabled) {
                when (operation) {
                    is Expiring,
                    is Operation.Clear,
                    is Operation.Invalidate -> assertEqualsWithContext(
                            mockReturnedResponseWrapper,
                            responseWrapper,
                            "The returned observable did not match",
                            "Failure for operation $operation"
                    )

                    else -> verifyDoNotCache(
                            operation,
                            isCacheEnabled,
                            responseWrapper
                    )
                }
            } else {
                verifyDoNotCache(
                        operation,
                        isCacheEnabled,
                        responseWrapper
                )
            }
        }
    }

    private fun prepareGetCachedResponse(operation: Expiring) {
        whenever(mockCacheManager.getCachedResponse(
                eq(mockUpstream),
                eq(mockInstructionToken),
                eq(operation),
                eq(mockStart)
        )).thenReturn(mockReturnedObservable)
    }

    private fun prepareClearCache(operation: Operation.Clear) {
        whenever(mockCacheManager.clearCache(
                eq(mockInstructionToken),
                eq(operation.typeToClear),
                eq(operation.clearOldEntriesOnly)
        )).thenReturn(mockReturnedObservable)
    }

    private fun prepareInvalidate() {
        whenever(mockCacheManager.invalidate(
                eq(mockInstructionToken)
        )).thenReturn(mockReturnedObservable)
    }

    private fun verifyDoNotCache(operation: Operation,
                                 isCacheEnabled: Boolean,
                                 responseWrapper: ResponseWrapper<Glitch>) {
        assertEqualsWithContext(
                mockMetadata.copy(cacheToken = CacheToken.notCached(
                        mockInstructionToken,
                        Date(1234L)
                )),
                responseWrapper.metadata,
                "Response wrapper metadata didn't match for operation == $operation and isCacheEnabled == $isCacheEnabled"
        )
    }

}
