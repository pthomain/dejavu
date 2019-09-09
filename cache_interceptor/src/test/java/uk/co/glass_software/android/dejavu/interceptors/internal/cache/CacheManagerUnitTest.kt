package uk.co.glass_software.android.dejavu.interceptors.internal.cache

import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Offline
import uk.co.glass_software.android.dejavu.configuration.ErrorFactory
import uk.co.glass_software.android.dejavu.configuration.Serialiser
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.DatabaseManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus.*
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.interceptors.internal.response.EmptyResponseFactory
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.retrofit.annotations.CacheException
import uk.co.glass_software.android.dejavu.retrofit.annotations.CacheException.Type.SERIALISATION
import uk.co.glass_software.android.dejavu.test.*
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import java.util.*

class CacheManagerUnitTest {

    private lateinit var mockErrorFactory: ErrorFactory<Glitch>
    private lateinit var mockSerialiser: Serialiser
    private lateinit var mockDatabaseManager: DatabaseManager<Glitch>
    private lateinit var mockEmptyResponseFactory: EmptyResponseFactory<Glitch>
    private lateinit var mockDateFactory: (Long?) -> Date
    private lateinit var mockNetworkGlitch: Glitch
    private lateinit var mockSerialisationGlitch: Glitch
    private lateinit var mockNetworkMetadata: CacheMetadata<Glitch>
    private lateinit var mockCacheMetadata: CacheMetadata<Glitch>
    private lateinit var expectedExpiryDate: Date

    private val mockSerialisedString = "mockSerialisedString"

    private val defaultDurationInMillis = 500L
    private val now = Date(1000L)
    private val start = 100L
    private val callDuration = 5000L

    private lateinit var target: CacheManager<Glitch>

    private fun setUp() {
        mockErrorFactory = mock()
        mockSerialiser = mock()
        mockDatabaseManager = mock()
        mockEmptyResponseFactory = mock()
        mockDateFactory = mock()
        mockNetworkGlitch = mock()
        mockSerialisationGlitch = mock()

        target = CacheManager(
                mockErrorFactory,
                mockSerialiser,
                mockDatabaseManager,
                mockEmptyResponseFactory,
                mockDateFactory,
                defaultDurationInMillis,
                mock()
        )
    }

    @Test
    fun testClearCache() {
        var iteration = 0
        trueFalseSequence { hasTypeToClear ->
            trueFalseSequence { clearStaleEntriesOnly ->
                testClearCache(
                        iteration++,
                        if (hasTypeToClear) TestResponse::class.java else null,
                        clearStaleEntriesOnly
                )
            }
        }
    }

    private fun testClearCache(iteration: Int,
                               typeToClear: Class<*>?,
                               clearStaleEntriesOnly: Boolean) {
        setUp()
        val context = "iteration = $iteration\n" +
                "typeToClear = $typeToClear,\n" +
                "clearStaleEntriesOnly = $clearStaleEntriesOnly"

        val instructionToken = instructionToken()
        val mockResponseWrapper = mock<ResponseWrapper<Glitch>>()

        whenever(mockEmptyResponseFactory.emptyResponseWrapperSingle(
                eq(instructionToken)
        )).thenReturn(Single.just(mockResponseWrapper))

        val actualResponseWrapper = target.clearCache(
                instructionToken,
                typeToClear,
                clearStaleEntriesOnly
        ).blockingFirst()

        verifyWithContext(mockDatabaseManager, context).clearCache(
                typeToClear,
                clearStaleEntriesOnly
        )

        assertEqualsWithContext(
                mockResponseWrapper,
                actualResponseWrapper,
                "Returned response wrapper didn't match",
                context
        )
    }

    @Test
    fun testInvalidate() {
        setUp()
        val instructionToken = instructionToken()
        val mockResponseWrapper = mock<ResponseWrapper<Glitch>>()

        whenever(mockEmptyResponseFactory.emptyResponseWrapperSingle(
                eq(instructionToken)
        )).thenReturn(Single.just(mockResponseWrapper))

        val actualResponseWrapper = target.invalidate(instructionToken).blockingFirst()

        verify(mockDatabaseManager).invalidate(eq(instructionToken))

        assertEqualsWithContext(
                mockResponseWrapper,
                actualResponseWrapper,
                "Returned response wrapper didn't match"
        )
    }

    @Test
    fun testGetCachedResponse() {
        var iteration = 0
        operationSequence { operation ->
            if (operation is Expiring) {
                trueFalseSequence { hasCachedResponse ->
                    trueFalseSequence { networkCallFails ->
                        trueFalseSequence { serialisationFails ->
                            trueFalseSequence { isResponseStale ->
                                testGetCachedResponse(
                                        iteration++,
                                        operation,
                                        hasCachedResponse,
                                        networkCallFails,
                                        serialisationFails,
                                        hasCachedResponse && isResponseStale
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun testGetCachedResponse(iteration: Int,
                                      operation: Expiring,
                                      hasCachedResponse: Boolean,
                                      networkCallFails: Boolean,
                                      serialisationFails: Boolean,
                                      isResponseStale: Boolean) {
        setUp()
        val context = "iteration = $iteration,\n" +
                "operation = ${operation.type},\n" +
                "operation.freshOnly = ${operation.freshOnly},\n" +
                "hasCachedResponse = $hasCachedResponse\n" +
                "isResponseStale = $isResponseStale\n" +
                "networkCallFails = $networkCallFails\n" +
                "serialisationFails = $serialisationFails\n"

        val instructionToken = instructionToken(operation)

        mockNetworkMetadata = CacheMetadata(
                instructionToken,
                if (networkCallFails) mockNetworkGlitch else null,
                CacheMetadata.Duration(0, callDuration.toInt(), 0)
        )

        mockCacheMetadata = mockNetworkMetadata.copy(exception = null)

        val mockResponse = mock<TestResponse>()
        val mockResponseWrapper = defaultResponseWrapper(
                mockNetworkMetadata,
                if (networkCallFails) null else mockResponse
        )
        val mockEmptyResponseWrapper = mock<ResponseWrapper<Glitch>>()
        val mockCachedResponseWrapper = mock<ResponseWrapper<Glitch>>()

        val isResponseStaleOverall = isResponseStale || operation is Expiring.Refresh
        whenever(mockCachedResponseWrapper.metadata).thenReturn(
                mockCacheMetadata.copy(instructionToken.copy(status = if (isResponseStaleOverall) STALE else NETWORK))
        )

        whenever(mockDatabaseManager.getCachedResponse(
                eq(instructionToken),
                eq(start)
        )).thenReturn(if (hasCachedResponse) mockCachedResponseWrapper else null)

        whenever(mockDateFactory.invoke(isNull())).thenReturn(now)

        if (operation is Offline) {
            if (!hasCachedResponse) {
                whenever(mockEmptyResponseFactory.emptyResponseWrapperSingle(
                        eq(instructionToken)
                )).thenReturn(Single.just(mockEmptyResponseWrapper))
            }
        } else if (!hasCachedResponse || isResponseStaleOverall) {
            prepareFetchAndCache(
                    operation,
                    mockResponse,
                    if (hasCachedResponse) mockCachedResponseWrapper else null,
                    hasCachedResponse,
                    networkCallFails,
                    serialisationFails
            )
        }

        val testObserver = TestObserver<ResponseWrapper<Glitch>>()

        target.getCachedResponse(
                Observable.just(mockResponseWrapper),
                instructionToken,
                operation,
                start
        ).subscribe(testObserver)

        if (operation is Offline) {
            if (hasCachedResponse) {
                assertEqualsWithContext(
                        mockCachedResponseWrapper,
                        getSingleActualResponse(context, testObserver),
                        "The response didn't match",
                        context
                )
            } else {
                assertEqualsWithContext(
                        mockEmptyResponseWrapper,
                        getSingleActualResponse(context, testObserver),
                        "The response should be empty when no cached response exists and the operation is OFFLINE",
                        context
                )
            }
        } else {
            verifyFetchAndCache(
                    testObserver,
                    context,
                    operation,
                    instructionToken,
                    hasCachedResponse,
                    isResponseStaleOverall,
                    mockResponse,
                    mockResponseWrapper,
                    mockCachedResponseWrapper,
                    networkCallFails,
                    serialisationFails
            )
        }
    }

    private fun getSingleActualResponse(context: String,
                                        testObserver: TestObserver<ResponseWrapper<Glitch>>): ResponseWrapper<Glitch> {
        assertTrueWithContext(
                testObserver.errorCount() == 0,
                "Expected no error",
                context
        )
        assertTrueWithContext(
                testObserver.valueCount() == 1,
                "Expected exactly one response",
                context
        )

        return testObserver.values().first()
    }

    private fun getResponsePair(context: String,
                                testObserver: TestObserver<ResponseWrapper<Glitch>>,
                                serialisationFails: Boolean,
                                networkCallFails: Boolean): Pair<ResponseWrapper<Glitch>, ResponseWrapper<Glitch>?> {
        assertEqualsWithContext(
                null,
                testObserver.errors().firstOrNull(),
                "Expected no error",
                context
        )

        assertEqualsWithContext(
                2,
                testObserver.valueCount(),
                "Expected exactly two responses",
                context
        )

        val secondResponse = testObserver.values()[1]

        if (!networkCallFails && serialisationFails) {
            assertNullWithContext(
                    secondResponse.response,
                    "Expected a null response",
                    context
            )

            assertNotNullWithContext(
                    secondResponse.metadata.exception,
                    "Expected an error",
                    context
            )

            assertEqualsWithContext(
                    mockSerialisationGlitch,
                    secondResponse.metadata.exception,
                    "Expected a serialisation error",
                    context
            )
        }

        return Pair(testObserver.values()[0], secondResponse)
    }

    private fun prepareFetchAndCache(operation: Expiring,
                                     mockResponse: TestResponse,
                                     mockCachedResponseWrapper: ResponseWrapper<Glitch>?,
                                     hasCachedResponse: Boolean,
                                     networkCallFails: Boolean,
                                     serialisationFails: Boolean) {
        prepareUpdateNetworkCallMetadata(
                hasCachedResponse,
                networkCallFails,
                operation,
                mockCachedResponseWrapper
        )

        prepareSerialise(
                operation,
                serialisationFails,
                hasCachedResponse,
                mockResponse,
                mockCachedResponseWrapper
        )
    }

    private fun prepareUpdateNetworkCallMetadata(hasCachedResponse: Boolean,
                                                 firstNetworkCallFails: Boolean,
                                                 operation: Expiring,
                                                 mockCachedResponseWrapper: ResponseWrapper<Glitch>?) {
        if (!firstNetworkCallFails) {
            val timeToLiveInMs = operation.durationInMillis ?: defaultDurationInMillis
            val expiryTimeStamp = now.time + timeToLiveInMs

            expectedExpiryDate = Date(expiryTimeStamp)
            whenever(mockDateFactory.invoke(eq(expiryTimeStamp)))
                    .thenReturn(expectedExpiryDate)
        }

        whenever(mockDatabaseManager.shouldEncryptOrCompress(
                if (hasCachedResponse) eq(mockCachedResponseWrapper) else isNull(),
                eq(operation)
        )).thenReturn(Pair(true, true))
    }

    private fun prepareSerialise(operation: Expiring,
                                 serialisationFails: Boolean,
                                 hasCachedResponse: Boolean,
                                 response: TestResponse,
                                 mockCachedResponseWrapper: ResponseWrapper<Glitch>?) {
        whenever(mockSerialiser.canHandleType(
                eq(TestResponse::class.java)
        )).thenReturn(true)

        whenever(mockSerialiser.serialise(eq(response)))
                .thenReturn(if (serialisationFails) null else mockSerialisedString)

        if (serialisationFails) {
            whenever(mockErrorFactory.getError(
                    eq(CacheException(
                            SERIALISATION,
                            "Could not serialise ${TestResponse::class.java.simpleName}: provided serialiser does not support the type. This response will not be cached."
                    )))).thenReturn(mockSerialisationGlitch)
        } else {
            whenever(mockDatabaseManager.cache(
                    any(),
                    if (hasCachedResponse) eq(mockCachedResponseWrapper) else isNull()
            )).thenReturn(Completable.complete())
        }
    }

    private fun verifyFetchAndCache(testObserver: TestObserver<ResponseWrapper<Glitch>>,
                                    context: String,
                                    operation: Expiring,
                                    instructionToken: CacheToken,
                                    hasCachedResponse: Boolean,
                                    isResponseStale: Boolean,
                                    expectedResponse: TestResponse,
                                    mockResponseWrapper: ResponseWrapper<Glitch>,
                                    mockCachedResponseWrapper: ResponseWrapper<Glitch>?,
                                    networkCallFails: Boolean,
                                    serialisationFails: Boolean) {
        val hasSingleResponse = (isResponseStale && operation.freshOnly)
                || !hasCachedResponse
                || !isResponseStale

        val actualResponseWrapper = if (hasSingleResponse) {
            getSingleActualResponse(context, testObserver)
        } else {
            val (firstResponse, secondResponse) = getResponsePair(
                    context,
                    testObserver,
                    serialisationFails,
                    networkCallFails
            )

            assertEqualsWithContext(
                    mockCachedResponseWrapper,
                    firstResponse,
                    "The first returned response should be the cached one",
                    context
            )

            secondResponse
        }!!

        verifyFetchAndCacheResponse(
                context,
                operation,
                instructionToken,
                actualResponseWrapper,
                expectedResponse,
                testObserver.errors().firstOrNull() as Glitch?,
                mockResponseWrapper,
                mockCachedResponseWrapper,
                hasCachedResponse,
                isResponseStale,
                networkCallFails,
                serialisationFails
        )
    }

    private fun verifyFetchAndCacheResponse(context: String,
                                            operation: Expiring,
                                            instructionToken: CacheToken,
                                            actualResponseWrapper: ResponseWrapper<Glitch>,
                                            expectedResponse: TestResponse,
                                            actualException: Glitch?,
                                            mockResponseWrapper: ResponseWrapper<Glitch>,
                                            mockCachedResponseWrapper: ResponseWrapper<Glitch>?,
                                            hasCachedResponse: Boolean,
                                            isResponseStale: Boolean,
                                            networkCallFails: Boolean,
                                            serialisationFails: Boolean) {
        val expectedResponseWrapper = if (hasCachedResponse) {
            if (isResponseStale) {
                if (networkCallFails) {
                    if (operation.freshOnly) {
                        mockResponseWrapper.copy(
                                response = null,
                                metadata = mockCacheMetadata.copy(
                                        exception = mockNetworkGlitch,
                                        cacheToken = instructionToken.copy(
                                                status = COULD_NOT_REFRESH,
                                                fetchDate = now
                                        )
                                )
                        )
                    } else {
                        mockResponseWrapper.copy(
                                response = null,
                                metadata = mockCacheMetadata.copy(
                                        exception = mockNetworkGlitch,
                                        cacheToken = instructionToken.copy(
                                                status = COULD_NOT_REFRESH,
                                                fetchDate = now
                                        ))
                        )
                    }
                } else if (serialisationFails) {
                    mockResponseWrapper.copy(
                            response = null,
                            metadata = mockCacheMetadata.copy(
                                    exception = mockSerialisationGlitch,
                                    cacheToken = instructionToken.copy(
                                            status = COULD_NOT_REFRESH,
                                            fetchDate = now
                                    )
                            )
                    )
                } else {
                    verifyCachedResponse(
                            context,
                            instructionToken,
                            expectedResponse,
                            actualResponseWrapper,
                            mockResponseWrapper,
                            mockCachedResponseWrapper,
                            hasCachedResponse
                    )
                }
            } else {
                mockCachedResponseWrapper
            }
        } else {
            if (networkCallFails) {
                mockResponseWrapper.copy(
                        response = null,
                        metadata = mockCacheMetadata.copy(
                                exception = mockNetworkGlitch,
                                cacheToken = instructionToken.copy(
                                        status = EMPTY,
                                        fetchDate = now
                                )
                        )
                )
            } else if (serialisationFails) {
                mockResponseWrapper.copy(
                        response = null,
                        metadata = mockCacheMetadata.copy(
                                exception = mockSerialisationGlitch,
                                cacheToken = instructionToken.copy(
                                        status = EMPTY,
                                        fetchDate = now
                                )
                        )
                )
            } else {
                verifyCachedResponse(
                        context,
                        instructionToken,
                        expectedResponse,
                        actualResponseWrapper,
                        mockResponseWrapper,
                        mockCachedResponseWrapper,
                        hasCachedResponse
                )
            }
        }

        assertEqualsWithContext(
                expectedResponseWrapper,
                actualResponseWrapper,
                "Returned response wrapper didn't match",
                context
        )
    }

    private fun verifyCachedResponse(context: String,
                                     instructionToken: CacheToken,
                                     expectedResponse: TestResponse?,
                                     actualResponseWrapper: ResponseWrapper<Glitch>,
                                     mockResponseWrapper: ResponseWrapper<Glitch>,
                                     mockCachedResponseWrapper: ResponseWrapper<Glitch>?,
                                     hasCachedResponse: Boolean): ResponseWrapper<Glitch> {
        verify(mockDatabaseManager).cache(
                eq(actualResponseWrapper.copy(
                        response = mockSerialisedString,
                        responseClass = String::class.java
                )),
                if (hasCachedResponse) eq(mockCachedResponseWrapper) else isNull()
        )

        val expectedResponseWrapper = if (hasCachedResponse) {
            mockResponseWrapper.copy(
                    response = expectedResponse,
                    metadata = mockCacheMetadata.copy(
                            exception = null,
                            cacheToken = instructionToken.copy(
                                    status = REFRESHED,
                                    fetchDate = now,
                                    cacheDate = now,
                                    expiryDate = expectedExpiryDate
                            ))
            )
        } else {
            mockResponseWrapper.copy(
                    response = expectedResponse,
                    metadata = mockCacheMetadata.copy(
                            exception = null,
                            cacheToken = instructionToken.copy(
                                    status = NETWORK,
                                    fetchDate = now,
                                    cacheDate = now,
                                    expiryDate = expectedExpiryDate
                            ))
            )
        }

        assertEqualsWithContext(
                expectedResponseWrapper.metadata.cacheToken,
                actualResponseWrapper.metadata.cacheToken,
                "Cache token didn't match",
                context
        )

        assertEqualsWithContext(
                expectedResponseWrapper,
                actualResponseWrapper,
                "Response wrapper cached by the manager didn't match",
                context
        )

        return expectedResponseWrapper
    }
}