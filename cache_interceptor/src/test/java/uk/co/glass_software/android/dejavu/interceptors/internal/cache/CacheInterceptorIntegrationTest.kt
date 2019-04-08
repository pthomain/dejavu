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
    fun `GIVEN that the instruction is DO_NOT_CACHE THEN the returned response has not been cached`() {
        setUpConfiguration()

    }

    @Test
    fun `GIVEN that the instruction is CACHE AND the cache is not enabled THEN the returned response has not been cached`() {
        setUpConfiguration(false)

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
    fun `GIVEN that the instruction is CLEAR AND the cache contains 2 responses of different types THEN the resulting status is DONE AND the target type response is null AND the other response is not null`() {
        setUpConfiguration()

    }

}