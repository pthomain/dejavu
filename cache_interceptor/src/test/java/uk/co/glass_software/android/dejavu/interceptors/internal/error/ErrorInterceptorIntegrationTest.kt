package uk.co.glass_software.android.dejavu.interceptors.internal.error

import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Cache
import uk.co.glass_software.android.dejavu.injection.integration.component.IntegrationCacheComponent
import uk.co.glass_software.android.dejavu.injection.module.CacheModule
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.test.*
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse

internal class ErrorInterceptorIntegrationTest
    : BaseIntegrationTest<CacheModule.Function2<CacheToken, Long, ErrorInterceptor<Glitch>>>(IntegrationCacheComponent::errorInterceptorFactory) {

    private lateinit var targetErrorInterceptor: ErrorInterceptor<Glitch>

    @Before
    fun setUp() {
        targetErrorInterceptor = target.get(
                instructionToken(Cache()),
                1234L
        )
    }

    private fun apply(observable: Observable<Any>) =
            targetErrorInterceptor.apply(observable).blockingFirst()

    @Test
    fun testApplyWithResponse() {
        val value = Object()

        val result = apply(Observable.just(value))

        assertEqualsWithContext(
                TestResponse::class.java,
                result.responseClass,
                "Response class didn't match"
        )

        assertEqualsWithContext(
                TestResponse::class.java,
                result.responseClass,
                "Response didn't match"
        )

        val metadata = result.metadata

        assertNullWithContext(
                metadata.exception,
                "Metadata should not have an exception"
        )

        assertEqualsWithContext(
                instructionToken(Cache()),
                metadata.cacheToken,
                "Cache token didn't match"
        )
    }

    @Test
    fun testApplyWithEmpty() {
        testApplyWithException(
                Observable.empty(),
                NoSuchElementException("Response was empty")
        )
    }

    @Test
    fun testApplyWithError() {
        val error = NullPointerException("error")
        testApplyWithException(
                Observable.error(error),
                error
        )
    }

    private fun testApplyWithException(observable: Observable<Any>,
                                       exception: Throwable) {
        val result = apply(observable)

        assertEqualsWithContext(
                TestResponse::class.java,
                result.responseClass,
                "Response class didn't match"
        )

        assertNullWithContext(
                result.response,
                "Response should be null"
        )

        val metadata = result.metadata

        assertNotNullWithContext(
                metadata.exception,
                "Metadata should have an exception"
        )

        assertEqualsWithContext(
                true,
                metadata.exception is Glitch,
                "Exception should be Glitch"
        )

        val glitch = metadata.exception as Glitch

        assertGlitchWithContext(
                Glitch(exception),
                glitch,
                "Glitch didn't match"
        )

        assertEqualsWithContext(
                instructionToken(Cache()),
                metadata.cacheToken,
                "Cache token didn't match"
        )
    }
}