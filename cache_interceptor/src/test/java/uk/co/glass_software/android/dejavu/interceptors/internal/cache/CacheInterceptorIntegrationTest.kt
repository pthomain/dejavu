package uk.co.glass_software.android.dejavu.interceptors.internal.cache

import org.junit.Test
import uk.co.glass_software.android.dejavu.injection.module.CacheModule.Function2
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.test.BaseIntegrationTest

internal class CacheInterceptorIntegrationTest : BaseIntegrationTest<Function2<CacheToken, Long, CacheInterceptor<Glitch>>>(
        { it.cacheInterceptorFactory() },
        false
) {

    //TODO

    private fun setUpConfiguration(isCacheEnabled: Boolean = true) {
        setUpWithConfiguration(configuration.copy(isCacheEnabled = isCacheEnabled))
    }

    @Test
    fun `GIVEN that the cache is not enabled THEN the returned response has not been cached`() {
        setUpConfiguration(false)

    }

    @Test
    fun `GIVEN that the instruction is DO_NOT_CACHE THEN the returned response has not been cached`() {
        setUpConfiguration()

    }

    @Test
    fun `GIVEN that the instruction is INVALIDATE AND the cache contains a FRESH response THEN the resulting status is DONE AND the returned response is STALE`() {
        setUpConfiguration()

    }

    @Test
    fun `GIVEN that the instruction is INVALIDATE AND the cache does not contains a response THEN the resulting status is DONE`() {
        setUpConfiguration()

    }

    @Test
    fun `GIVEN instruction CLEAR_TARGET AND cache contains 2 responses of different types with the same expiry date THEN status is DONE AND the target type response is null AND the other response is not null`() {
        setUpConfiguration()

    }

    @Test
    fun `GIVEN instruction CLEAR_TARGET AND cache contains 2 responses of different types with different expiry dates THEN status is DONE AND the target type response is null AND the other response is not null`() {
        setUpConfiguration()

    }

    @Test
    fun `GIVEN that the instruction is CLEAR_STALE AND the cache contains 2 responses of the same type AND with different expiry date THEN the resulting status is DONE AND only the STALE one is cleared`() {
        setUpConfiguration()

    }

    @Test
    fun `GIVEN that the instruction is CLEAR_STALE AND the cache contains 2 responses of different types AND with different expiry date THEN the resulting status is DONE AND only the STALE one is cleared`() {
        setUpConfiguration()

    }

    @Test
    fun `GIVEN that the instruction is CLEAR_ALL AND the cache contains 2 responses of different types AND with the same expiry dates THEN the resulting status is DONE AND both responses are null`() {
        setUpConfiguration()

    }

    @Test
    fun `GIVEN that the instruction is CLEAR_ALL AND the cache contains 2 responses of different types AND with the different expiry dates THEN the resulting status is DONE AND both responses are null`() {
        setUpConfiguration()

    }

    @Test
    fun `GIVEN that the instruction is CLEAR_ALL_STALE AND the cache contains 2 responses of different types AND with the different expiry dates THEN the resulting status is DONE AND only the STALE response is null`() {
        setUpConfiguration()

    }

    @Test
    fun `GIVEN that the instruction is CLEAR_ALL_STALE AND the cache contains 2 responses of different types AND with the same STALE expiry date THEN the resulting status is DONE AND only both responses are null`() {
        setUpConfiguration()

    }

    @Test
    fun `GIVEN that the instruction is CACHE`() {
        setUpConfiguration()

    }

    @Test
    fun `GIVEN that the instruction is REFRESH`() {
        setUpConfiguration()

    }

    @Test
    fun `GIVEN that the instruction is OFFLINE`() {
        setUpConfiguration()

    }

}