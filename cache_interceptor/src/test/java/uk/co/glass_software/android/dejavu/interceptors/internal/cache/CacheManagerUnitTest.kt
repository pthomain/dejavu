package uk.co.glass_software.android.dejavu.interceptors.internal.cache

import com.nhaarman.mockitokotlin2.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Offline
import uk.co.glass_software.android.dejavu.configuration.ErrorFactory
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
import uk.co.glass_software.android.shared_preferences.persistence.serialisation.Serialiser
import java.util.*

class CacheManagerUnitTest {

    private lateinit var mockErrorFactory: ErrorFactory<Glitch>
    private lateinit var mockSerialiser: Serialiser
    private lateinit var mockDatabaseManager: DatabaseManager<Glitch>
    private lateinit var mockEmptyResponseFactory: EmptyResponseFactory<Glitch>
    private lateinit var mockDateFactory: (Long?) -> Date
    private lateinit var mockGlitch: Glitch
    private lateinit var mockMetadata: CacheMetadata<Glitch>
    private lateinit var expectedExpiryDate: Date

    private val mockSerialisedString = "mockSerialisedString"

    private val defaultDurationInMillis = 500L
    private val now = Date(1000L)
    private val start = 100L
    private val callDuration = 5000L

    private lateinit var target: CacheManager<Glitch>

    @Before
    fun setUp() {
        mockErrorFactory = mock()
        mockSerialiser = mock()
        mockDatabaseManager = mock()
        mockEmptyResponseFactory = mock()
        mockDateFactory = mock()
        mockGlitch = mock()

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
            trueFalseSequence { clearOlderEntriesOnly ->
                testClearCache(
                        iteration++,
                        if (hasTypeToClear) TestResponse::class.java else null,
                        clearOlderEntriesOnly
                )
            }
        }
    }

    private fun testClearCache(iteration: Int,
                               typeToClear: Class<*>?,
                               clearOlderEntriesOnly: Boolean) {
        val context = "iteration = $iteration\n" +
                "typeToClear = $typeToClear,\n" +
                "clearOlderEntriesOnly = $clearOlderEntriesOnly"

        val instructionToken = instructionToken()
        val mockResponseWrapper = mock<ResponseWrapper<Glitch>>()

        whenever(mockEmptyResponseFactory.emptyResponseWrapperSingle(
                eq(instructionToken)
        )).thenReturn(Single.just(mockResponseWrapper))

        val actualResponseWrapper = target.clearCache(
                instructionToken,
                typeToClear,
                clearOlderEntriesOnly
        ).blockingFirst()

        verifyWithContext(mockDatabaseManager, context).clearCache(
                typeToClear,
                clearOlderEntriesOnly
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
                                trueFalseSequence { isCouldNotRefresh ->
                                    testGetCachedResponse(
                                            iteration++,
                                            operation,
                                            hasCachedResponse,
                                            networkCallFails,
                                            serialisationFails,
                                            isResponseStale && hasCachedResponse,
                                            isCouldNotRefresh && (isResponseStale && hasCachedResponse)
                                    )
                                }
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
                                      isResponseStale: Boolean,
                                      isCouldNotRefresh: Boolean) {
        val context = "iteration = $iteration,\n" +
                "operation = $operation,\n" +
                "hasCachedResponse = $hasCachedResponse\n" +
                "networkCallFails = $networkCallFails\n" +
                "serialisationFails = $serialisationFails\n" +
                "isResponseStale = $isResponseStale\n" +
                "isCouldNotRefresh = $isCouldNotRefresh"

        System.out.println(context)

        val instructionToken = instructionToken(operation)

        mockMetadata = CacheMetadata(
                instructionToken,
                if (networkCallFails) mockGlitch else null,
                CacheMetadata.Duration(0, callDuration.toInt(), 0)
        )

        val mockResponse = mock<TestResponse>()
        val mockResponseWrapper = defaultResponseWrapper(mockMetadata, mockResponse)
        val mockSerialisedResponseWrapper = mockResponseWrapper.copy(
                response = mockSerialisedString,
                responseClass = String::class.java
        )
        val mockEmptyResponseWrapper = mock<ResponseWrapper<Glitch>>()
        val mockCachedResponseWrapper = mock<ResponseWrapper<Glitch>>()

        whenever(mockDatabaseManager.getCachedResponse(
                eq(instructionToken),
                eq(start)
        )).thenReturn(if (hasCachedResponse) mockCachedResponseWrapper else null)

        //TODO test existing duration
        whenever(mockDateFactory.invoke(isNull())).thenReturn(now)

        if (operation is Offline) {
            if (!hasCachedResponse) {
                whenever(mockEmptyResponseFactory.emptyResponseWrapperSingle(
                        eq(instructionToken)
                )).thenReturn(Single.just(mockEmptyResponseWrapper))
            }
        } else if (!hasCachedResponse || isResponseStale) {
            prepareFetchAndCache(
                    operation,
                    mockSerialisedResponseWrapper,
                    mockResponse,
                    if (hasCachedResponse) mockCachedResponseWrapper else null,
                    hasCachedResponse,
                    networkCallFails,
                    serialisationFails,
                    instructionToken
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
            if (!hasCachedResponse || isResponseStale) {
                verifyFetchAndCache(
                        testObserver,
                        context,
                        operation,
                        hasCachedResponse,
                        isResponseStale,
                        mockResponseWrapper,
                        mockCachedResponseWrapper,
                        networkCallFails,
                        serialisationFails
                )
            } else {
                assertEqualsWithContext(
                        mockCachedResponseWrapper,
                        getSingleActualResponse(context, testObserver),
                        "The response didn't match",
                        context
                )
            }
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
                                serialisationFails: Boolean): Pair<ResponseWrapper<Glitch>, ResponseWrapper<Glitch>?> {


        val secondResponse = if (serialisationFails) {
            assertTrueWithContext(
                    testObserver.errorCount() == 1,
                    "Expected no error",
                    context
            )

            null
        } else {
            assertTrueWithContext(
                    testObserver.errorCount() == 0,
                    "Expected no error",
                    context
            )

            assertTrueWithContext(
                    testObserver.valueCount() == 2,
                    "Expected exactly two responses",
                    context
            )
            testObserver.values()[1]
        }

        return Pair(testObserver.values()[0], secondResponse)
    }

    private fun prepareFetchAndCache(operation: Expiring,
                                     mockSerialisedResponseWrapper: ResponseWrapper<Glitch>,
                                     mockResponse: TestResponse,
                                     mockCachedResponseWrapper: ResponseWrapper<Glitch>?,
                                     hasCachedResponse: Boolean,
                                     networkCallFails: Boolean,
                                     serialisationFails: Boolean,
                                     instructionToken: CacheToken) {
        prepareUpdateNetworkCallMetadata(
                hasCachedResponse,
                networkCallFails,
                operation,
                mockCachedResponseWrapper
        )

        prepareSerialise(
                serialisationFails,
                mockResponse
        )

        if (!serialisationFails) {

            val expectedStatus = if (networkCallFails)
                if (hasCachedResponse) COULD_NOT_REFRESH else EMPTY
            else
                if (hasCachedResponse) REFRESHED else FRESH

            val diskDuration = now.time.toInt() - start.toInt()

            val metadata = mockMetadata.copy(
                    CacheToken(
                            instructionToken.instruction,
                            expectedStatus,
                            true,
                            true,
                            instructionToken.requestMetadata,
                            now,
                            if (networkCallFails) null else now,
                            if (networkCallFails) null else expectedExpiryDate
                    ),
                    callDuration = CacheMetadata.Duration(
                            diskDuration,
                            callDuration.toInt() - diskDuration,
                            0
                    )
            )

            whenever(mockDatabaseManager.cache(
                    eq(instructionToken),
                    eq(operation),
                    eq(mockSerialisedResponseWrapper.copy(metadata = metadata)),
                    if (hasCachedResponse) eq(mockCachedResponseWrapper) else isNull()
            )).thenReturn(Completable.complete())
        }
    }

    private fun prepareUpdateNetworkCallMetadata(hasCachedResponse: Boolean,
                                                 networkCallFails: Boolean,
                                                 operation: Expiring,
                                                 mockCachedResponseWrapper: ResponseWrapper<Glitch>?) {
        if (!networkCallFails) {
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

    private fun prepareSerialise(serialisationFails: Boolean,
                                 response: TestResponse) {
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
                    )))).thenReturn(mockGlitch)
        }
    }

    private fun verifyFetchAndCache(testObserver: TestObserver<ResponseWrapper<Glitch>>,
                                    context: String,
                                    operation: Expiring,
                                    hasCachedResponse: Boolean,
                                    isResponseStale: Boolean,
                                    mockResponseWrapper: ResponseWrapper<Glitch>,
                                    mockCachedResponseWrapper: ResponseWrapper<Glitch>?,
                                    networkCallFails: Boolean,
                                    serialisationFails: Boolean) {
        val actualResponse = if ((isResponseStale && operation.freshOnly) || !hasCachedResponse) {
            if (serialisationFails)
                testObserver.values().firstOrNull()
            else
                getSingleActualResponse(context, testObserver)
        } else {
            val (firstResponse, secondResponse) = getResponsePair(
                    context,
                    testObserver,
                    serialisationFails
            )

            assertEqualsWithContext(
                    mockCachedResponseWrapper,
                    firstResponse,
                    "The first returned response should be the cached one",
                    context
            )

            if (serialisationFails)
                testObserver.values().firstOrNull()
            else
                secondResponse
        }

        verifyFetchAndCacheResponse(
                context,
                actualResponse,
                testObserver.errors().firstOrNull() as Glitch?,
                mockResponseWrapper,
                mockCachedResponseWrapper,
                hasCachedResponse,
                networkCallFails,
                serialisationFails
        )
    }

    private fun verifyFetchAndCacheResponse(context: String,
                                            actualResponse: ResponseWrapper<Glitch>?,
                                            actualException: Glitch?,
                                            mockResponseWrapper: ResponseWrapper<Glitch>,
                                            mockCachedResponseWrapper: ResponseWrapper<Glitch>?,
                                            hasCachedResponse: Boolean,
                                            networkCallFails: Boolean,
                                            serialisationFails: Boolean) {






//        if (networkCallFails) {
//            if (hasCachedResponse) {
//                //TODO
////                assertEqualsWithContext(
////                        mockCachedResponseWrapper?.copy(
////                                metadata = mockMetadata.copy(mockCachedResponseWrapper.copy())
////                        ),
////                        actualResponse,
////                        "The cached response should have been returned"
////                )
//            } else {
//
//            }
//        } else if (serialisationFails) {
////            assertNullWithContext(
////                    actualResponse,
////                    "The response should be null when a serialisation error occurs",
////                    context
////            )
//        } else {
//            assertEqualsWithContext(
//                    mockResponseWrapper,
//                    actualResponse,
//                    "The response didn't match",
//                    context
//            )
//
//            assertNullWithContext(
//                    actualException,
//                    "The exception should be null",
//                    context
//            )
//        }
    }

    //TODO verify cache tokens and responses
}