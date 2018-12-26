package uk.co.glass_software.android.dejavu.retrofit

import com.nhaarman.mockitokotlin2.mock
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver
import junit.framework.TestCase.assertTrue
import org.junit.Test
import retrofit2.CallAdapter
import retrofit2.http.GET
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.injection.integration.component.IntegrationCacheComponent
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.ErrorCode
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch.Companion.NON_HTTP_STATUS
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.*
import uk.co.glass_software.android.dejavu.retrofit.annotations.CacheException
import uk.co.glass_software.android.dejavu.retrofit.annotations.CacheException.Type.ANNOTATION
import uk.co.glass_software.android.dejavu.retrofit.annotations.DoNotCache
import uk.co.glass_software.android.dejavu.test.BaseIntegrationTest
import uk.co.glass_software.android.dejavu.test.assertEqualsWithContext
import uk.co.glass_software.android.dejavu.test.assertNotNullWithContext
import uk.co.glass_software.android.dejavu.test.getAnnotation
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


@Suppress("UNCHECKED_CAST")
internal class ProcessingErrorAdapterIntegrationTest
    : BaseIntegrationTest<ProcessingErrorAdapter.Factory<Glitch>>(IntegrationCacheComponent::processingErrorAdapterFactory) {

    private lateinit var cacheException: CacheException
    private lateinit var cacheToken: CacheToken
    private lateinit var testObserver: TestObserver<Any>

    private lateinit var targetAdapter: ProcessingErrorAdapter<Glitch>

    private fun setUp(rxClass: Class<*>,
                      rxType: AnnotationProcessor.RxType) {
        val defaultAdapter = cacheComponent.defaultAdapterFactory().get(
                object : ParameterizedType {
                    override fun getRawType() = rxClass
                    override fun getOwnerType() = null
                    override fun getActualTypeArguments() = arrayOf<Type>(TestResponse::class.java)
                },
                arrayOf(
                        getAnnotation<GET>(listOf("/")),
                        getAnnotation<DoNotCache>(emptyList())
                ),
                retrofit
        ) as CallAdapter<Any, Any>

        cacheException = CacheException(
                ANNOTATION,
                "error"
        )

        cacheToken = instructionToken(CacheInstruction.Operation.DoNotCache)

        targetAdapter = target.create(
                defaultAdapter,
                cacheToken,
                1234L,
                rxType,
                cacheException
        )

        enqueueResponse("", 200)

        testObserver = TestObserver()
    }

    @Test
    fun testAdaptObservable() {
        setUp(Observable::class.java, OBSERVABLE)

        val adapted = targetAdapter.adapt(mock()) as Observable<Any>

        adapted.subscribe(testObserver)
        verifyObserver(testObserver)
    }

    @Test
    fun testAdaptSingle() {
        setUp(Single::class.java, SINGLE)

        val adapted = targetAdapter.adapt(mock()) as Single<Any>

        adapted.subscribe(testObserver)
        verifyObserver(testObserver)
    }

    @Test
    fun testAdaptCompletable() {
        setUp(Completable::class.java, COMPLETABLE)

        val adapted = targetAdapter.adapt(mock()) as Completable

        adapted.subscribe(testObserver)
        verifyObserver(testObserver, true)
    }

    private fun verifyObserver(testObserver: TestObserver<Any>,
                               isCompletable: Boolean = false) {


        val exception = if (!isCompletable) {
            assertEqualsWithContext(
                    1,
                    testObserver.values().size,
                    "Observer should have one value"
            )

            val response = testObserver.values().first() as TestResponse

            assertNotNullWithContext(
                    response.metadata,
                    "Response should have metadata"
            )

            assertNotNullWithContext(
                    response.metadata.cacheToken,
                    "Response metadata should have a cache token"
            )

            assertNotNullWithContext(
                    response.metadata.exception,
                    "Response metadata should have an exception"
            )

            assertNotNullWithContext(
                    response.metadata.callDuration,
                    "Response metadata should have a call duration"
            )

            assertEqualsWithContext(
                    cacheToken,
                    response.metadata.cacheToken,
                    "Cache token should match instruction token"
            )

            response.metadata.exception
        } else {
            assertEqualsWithContext(
                    1,
                    testObserver.errors().size,
                    "Observer should have one error"
            )

            testObserver.errors().first()
        }

        assertTrue(
                "Exception should be a Glitch",
                exception is Glitch
        )

        val glitch = exception as Glitch

        assertEqualsWithContext(
                ErrorCode.CONFIG,
                glitch.errorCode,
                "Glitch error code should be CONFIG"
        )

        assertEqualsWithContext(
                "Configuration error",
                glitch.description,
                "Glitch error description didn't match"
        )

        assertEqualsWithContext(
                NON_HTTP_STATUS,
                glitch.httpStatus,
                "Glitch error HTTP status didn't match"
        )

        val cause = glitch.cause

        assertNotNullWithContext(
                cause,
                "Glitch cause should not be null"
        )

        assertTrue(
                "Glitch cause should be CacheException",
                cause is CacheException
        )

        val cacheException = cause as CacheException

        assertEqualsWithContext(
                CacheException.Type.ANNOTATION,
                cacheException.type,
                "Cache exception type should be ANNOTATION"
        )

        assertEqualsWithContext(
                "error",
                cacheException.message,
                "Cache exception message should be error"
        )
    }
}