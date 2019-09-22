package uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence.database

import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Cache
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Refresh
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.persistence.PersistenceManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus.FRESH
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus.STALE
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.test.BaseIntegrationTest
import uk.co.glass_software.android.dejavu.test.assertNotNullWithContext
import uk.co.glass_software.android.dejavu.test.assertNullWithContext
import uk.co.glass_software.android.dejavu.test.assertTrueWithContext
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import uk.co.glass_software.android.dejavu.test.network.model.User
import java.util.*

internal class DatabasePersistenceManagerIntegrationTest
    : BaseIntegrationTest<PersistenceManager<Glitch>>({ it.persistenceManager() }) {

    @Test
    fun `GIVEN that a response is not cached THEN it should not be returned`() {
        val cachedResponse = target.getCachedResponse(instructionToken(), 500)

        assertNullWithContext(
                cachedResponse,
                "Response should be null"
        )
    }

    @Test
    fun `GIVEN that a response is cached THEN it should be returned`() {
        val stubbedResponse = getStubbedTestResponse()

        target.cache(stubbedResponse, null)

        val cachedResponse = target.getCachedResponse(instructionToken(), 500)

        assertResponse(
                stubbedResponse,
                cachedResponse,
                FRESH
        )
    }

    @Test
    fun `GIVEN that a response has expired THEN it should have a STALE status`() {
        val stubbedResponse = getStubbedTestResponse(instructionToken(
                Cache(durationInMillis = -1L)
        ))

        val instructionToken = stubbedResponse.metadata.cacheToken

        target.cache(stubbedResponse, null)

        val cachedResponse = target.getCachedResponse(instructionToken, 500)

        assertResponse(
                stubbedResponse,
                cachedResponse,
                STALE,
                expiryDate = Date(NOW.time - 1L)
        )
    }

    @Test
    fun `GIVEN that a response has not expired AND the operation is INVALIDATE THEN it should have a STALE status`() {
        val stubbedResponse = getStubbedTestResponse()
        val instructionToken = stubbedResponse.metadata.cacheToken

        target.cache(stubbedResponse, null)
        target.invalidate(instructionToken)

        val cachedResponse = target.getCachedResponse(instructionToken, 500)

        assertResponse(
                stubbedResponse,
                cachedResponse,
                STALE,
                expiryDate = Date(0)
        )
    }

    @Test
    fun `GIVEN that a response has not expired AND the operation is REFRESH THEN it should have a STALE status`() {
        val stubbedResponse = getStubbedTestResponse()

        target.cache(stubbedResponse, null)

        val cachedResponse = target.getCachedResponse(instructionToken(), 500)

        assertResponse(
                stubbedResponse,
                cachedResponse,
                FRESH
        )

        val refreshToken = instructionToken(Refresh())
        val refreshResponse = target.getCachedResponse(refreshToken, 500)

        assertResponse(
                getStubbedTestResponse(refreshToken),
                refreshResponse,
                STALE,
                expiryDate = Date(0)
        )
    }

    @Test
    fun `GIVEN that a response has not expired AND the operation is not REFRESH or INVALIDATE THEN it should have a CACHED status`() {
        val stubbedResponse = getStubbedTestResponse()

        target.cache(stubbedResponse, null)

        val cachedResponse = target.getCachedResponse(instructionToken(), 500)

        assertResponse(
                stubbedResponse,
                cachedResponse,
                FRESH
        )
    }

    @Test
    fun `GIVEN that 2 different responses are cached AND the cache is cleared for one type THEN this type should not be returned AND the other type should be returned`() {
        val stubbedResponse = getStubbedTestResponse()
        val userResponse = getStubbedUserResponseWrapper()

        val (testResponseToken, userResponseToken) = cacheTwoResponses(stubbedResponse, userResponse)

        target.clearCache(TestResponse::class.java, false)

        val cachedTestResponseAfterClear = target.getCachedResponse(testResponseToken, 500)
        val cachedUserResponseAfterClear = target.getCachedResponse(userResponseToken, 500)

        assertNullWithContext(
                cachedTestResponseAfterClear,
                "Test response should have been cleared"
        )

        assertResponse(
                userResponse,
                cachedUserResponseAfterClear,
                FRESH
        )
    }

    @Test
    fun `GIVEN that 2 different responses are cached AND the whole cache is cleared THEN both type should not be returned`() {
        val stubbedResponse = getStubbedTestResponse()
        val userResponse = getStubbedUserResponseWrapper()

        val (testResponseToken, userResponseToken) = cacheTwoResponses(stubbedResponse, userResponse)

        target.clearCache(null, false)

        val cachedTestResponseAfterClear = target.getCachedResponse(testResponseToken, 500)
        val cachedUserResponseAfterClear = target.getCachedResponse(userResponseToken, 500)

        assertNullWithContext(
                cachedTestResponseAfterClear,
                "Test response should have been cleared"
        )

        assertNullWithContext(
                cachedUserResponseAfterClear,
                "User response should have been cleared"
        )
    }

    @Test
    fun `GIVEN that 2 responses of the same type are cached AND their respective URLs are different THEN both type should be returned independently`() {
        val firstResponse = getStubbedTestResponse(instructionToken(
                url = "http://test.com/testResponse1"
        ))

        val secondResponse = getStubbedTestResponse(instructionToken(
                url = "http://test.com/testResponse2"
        )).let {
            it.copy(
                    response = TestResponse().apply { addAll((it.response as TestResponse).take(5)) }
            )
        }

        val (firstResponseToken, secondResponseToken) = cacheTwoResponses(firstResponse, secondResponse)

        assertTrueWithContext(
                firstResponseToken.requestMetadata != secondResponseToken.requestMetadata,
                "Request metadata for 2 responses of the same type with different URLs should be different"
        )

        val cachedFirstResponse = target.getCachedResponse(firstResponseToken, 500)
        val cachedSecondResponse = target.getCachedResponse(secondResponseToken, 500)

        assertNotNullWithContext(
                cachedFirstResponse,
                "Test response should have been cached"
        )

        assertNotNullWithContext(
                cachedSecondResponse,
                "Test response should have been cached"
        )

        assertTrueWithContext(
                cachedFirstResponse!!.response != cachedSecondResponse!!.response,
                "Responses should have a different number of users"
        )
    }

    private fun cacheTwoResponses(
            firstResponse: ResponseWrapper<Glitch>,
            secondResponse: ResponseWrapper<Glitch>,
            firstResponseExpectedStatus: CacheStatus = FRESH,
            secondResponseExpectedStatus: CacheStatus = FRESH
    ): Pair<CacheToken, CacheToken> {
        val firstResponseToken = firstResponse.metadata.cacheToken
        val secondResponseToken = secondResponse.metadata.cacheToken

        target.cache(firstResponse, null)
        target.cache(secondResponse, null)

        val cachedFirstResponse = target.getCachedResponse(firstResponseToken, 500)
        val cachedSecondResponse = target.getCachedResponse(secondResponseToken, 500)

        assertResponse(
                firstResponse,
                cachedFirstResponse,
                firstResponseExpectedStatus
        )

        assertResponse(
                secondResponse,
                cachedSecondResponse,
                secondResponseExpectedStatus
        )

        return firstResponseToken to secondResponseToken
    }

    @Test
    fun `GIVEN that 2 responses of the same type are cached AND only one has expired AND the cache is cleared for older entries of that type THEN only the expired one should be cleared AND the other one should be returned`() {
        val freshStubbedResponse = getStubbedTestResponse(instructionToken(
                url = "http://test.com/testResponse1"
        ))

        val expiredExceptionStubbedResponse = freshStubbedResponse.copy(
                metadata = freshStubbedResponse.metadata.copy(
                        cacheToken = instructionToken(
                                Cache(durationInMillis = -1L),
                                url = "http://test.com/testResponse2"
                        )
                )
        )

        val (freshResponseToken, expiredResponseToken) = cacheTwoResponses(
                freshStubbedResponse,
                expiredExceptionStubbedResponse,
                secondResponseExpectedStatus = STALE
        )

        target.clearCache(TestResponse::class.java, true)

        val freshTestResponseAfterClear = target.getCachedResponse(freshResponseToken, 500)
        val expiredUserResponseAfterClear = target.getCachedResponse(expiredResponseToken, 500)

        assertNullWithContext(
                expiredUserResponseAfterClear,
                "Expired TestResponse should have been cleared"
        )

        assertResponse(
                freshStubbedResponse,
                freshTestResponseAfterClear,
                FRESH
        )
    }

    @Test
    fun `GIVEN that 4 responses of 2 different types are cached AND only one of each type has expired AND the whole cache is cleared for older entries THEN only the expired ones should be cleared`() {
        val testResponse = getStubbedTestResponse(instructionToken(
                url = "http://test.com/testResponse1"
        ))

        val userResponse = getStubbedUserResponseWrapper(
                url = "http://test.com/userResponse1"
        )

        val expiredTestResponse = getStubbedTestResponse(instructionToken(
                Cache(durationInMillis = -1),
                url = "http://test.com/testResponse2"
        ))

        val expiredUserResponse = getStubbedUserResponseWrapper(
                instructionToken(
                        Cache(durationInMillis = -1),
                        url = "http://test.com/userResponse2"
                ),
                url = "http://test.com/userResponse2"
        )

        val (testResponseToken, userResponseToken) = cacheTwoResponses(testResponse, userResponse)
        val (expiredTestResponseToken, expiredUserResponseToken) = cacheTwoResponses(
                expiredTestResponse,
                expiredUserResponse,
                STALE,
                STALE
        )

        val cachedTestResponse = target.getCachedResponse(testResponseToken, 500)
        val cachedUserResponse = target.getCachedResponse(userResponseToken, 500)

        val cachedExpiredTestResponse = target.getCachedResponse(expiredTestResponseToken, 500)
        val cachedExpiredUserResponse = target.getCachedResponse(expiredUserResponseToken, 500)

        assertResponse(
                testResponse,
                cachedTestResponse,
                FRESH
        )

        assertResponse(
                userResponse,
                cachedUserResponse,
                FRESH
        )

        assertResponse(
                testResponse.copy(metadata = testResponse.metadata.copy(cacheToken = instructionToken(
                        Cache(durationInMillis = -1),
                        url = "http://test.com/testResponse2"
                ))),
                cachedExpiredTestResponse,
                STALE
        )

        assertResponse(
                userResponse.copy(metadata = userResponse.metadata.copy(cacheToken = instructionToken(
                        Cache(durationInMillis = -1),
                        responseClass = User::class.java,
                        url = "http://test.com/userResponse2"
                ))),
                cachedExpiredUserResponse,
                STALE
        )

        target.clearCache(null, true)

        val cachedTestResponseAfterClear = target.getCachedResponse(testResponseToken, 500)
        val cachedUserResponseAfterClear = target.getCachedResponse(userResponseToken, 500)

        val cachedExpiredTestResponseAfterClear = target.getCachedResponse(expiredTestResponseToken, 500)
        val cachedExpiredUserResponseAfterClear = target.getCachedResponse(expiredUserResponseToken, 500)

        assertResponse(
                testResponse,
                cachedTestResponseAfterClear,
                FRESH
        )

        assertResponse(
                userResponse,
                cachedUserResponseAfterClear,
                FRESH
        )

        assertNullWithContext(
                cachedExpiredTestResponseAfterClear,
                "Expired response should have been cleared"
        )

        assertNullWithContext(
                cachedExpiredUserResponseAfterClear,
                "Expired response should have been cleared"
        )
    }
}
