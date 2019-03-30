package uk.co.glass_software.android.dejavu.interceptors.internal.cache.database

import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Cache
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Refresh
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.test.BaseIntegrationTest
import uk.co.glass_software.android.dejavu.test.assertNullWithContext
import uk.co.glass_software.android.dejavu.test.instructionToken
import java.util.*

internal class DatabaseManagerIntegrationTest : BaseIntegrationTest<DatabaseManager<Glitch>>({ it.databaseManager() }) {

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

        target.cache(stubbedResponse, null).blockingAwait()

        val cachedResponse = target.getCachedResponse(instructionToken(), 500)

        assertResponse(
                stubbedResponse,
                cachedResponse,
                CacheStatus.CACHED
        )
    }

    @Test
    fun `GIVEN that a response has expired THEN it should have a STALE status`() {
        val stubbedResponse = getStubbedTestResponse(instructionToken(
                Cache(durationInMillis = -1L)
        ))

        val instructionToken = stubbedResponse.metadata.cacheToken

        target.cache(stubbedResponse, null).blockingAwait()

        val cachedResponse = target.getCachedResponse(instructionToken, 500)

        assertResponse(
                stubbedResponse,
                cachedResponse,
                CacheStatus.STALE,
                expiryDate = Date(NOW.time - 1L)
        )
    }

    @Test
    fun `GIVEN that a response has not expired AND the operation is INVALIDATE THEN it should have a STALE status`() {
        val stubbedResponse = getStubbedTestResponse()
        val instructionToken = stubbedResponse.metadata.cacheToken

        target.cache(stubbedResponse, null).blockingAwait()
        target.invalidate(instructionToken)

        val cachedResponse = target.getCachedResponse(instructionToken, 500)

        assertResponse(
                stubbedResponse,
                cachedResponse,
                CacheStatus.STALE,
                expiryDate = Date(0)
        )
    }

    @Test
    fun `GIVEN that a response has not expired AND the operation is REFRESH THEN it should have a STALE status`() {
        val stubbedResponse = getStubbedTestResponse()

        target.cache(stubbedResponse, null).blockingAwait()

        val cachedResponse = target.getCachedResponse(instructionToken(), 500)

        assertResponse(
                stubbedResponse,
                cachedResponse,
                CacheStatus.CACHED
        )

        val refreshToken = instructionToken(Refresh())
        val refreshResponse = target.getCachedResponse(refreshToken, 500)

        assertResponse(
                getStubbedTestResponse(refreshToken),
                refreshResponse,
                CacheStatus.STALE,
                expiryDate = Date(0)
        )
    }

    @Test
    fun `GIVEN that a response has not expired AND the operation is not REFRESH or INVALIDATE THEN it should have a FRESH status`() {

    }

    @Test
    fun `GIVEN that a response is cached AND the cache is cleared for that type THEN it should not be returned AND the database should contain other types`() {

    }

    @Test
    fun `GIVEN that a response is cached AND the whole cache is cleared for that type THEN it should not be returned AND the database should be empty`() {

    }

    @Test
    fun `GIVEN that a response must be cached encrypted THEN it is returned encrypted`() {

    }

    @Test
    fun `GIVEN that a response must be cached compressed THEN it is returned compressed`() {

    }

    @Test
    fun `GIVEN that a response must be cached compressed and encrypted THEN it is returned compressed and encrypted`() {

    }

    @Test
    fun `GIVEN that a response must be cached encrypted as per previous response THEN it is returned encrypted`() {

    }

    @Test
    fun `GIVEN that a response must be cached compressed as per previous response THEN it is returned compressed`() {

    }

    @Test
    fun `GIVEN that a response must be cached compressed and encrypted as per previous response THEN it is returned compressed and encrypted`() {

    }

    @Test
    fun `GIVEN that a response must be cached as is as per previous response THEN it is returned as is`() {

    }

}