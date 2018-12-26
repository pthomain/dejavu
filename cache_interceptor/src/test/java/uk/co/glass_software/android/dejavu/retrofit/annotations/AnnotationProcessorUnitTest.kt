package uk.co.glass_software.android.dejavu.retrofit.annotations

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Type.DO_NOT_CACHE
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Type.OFFLINE
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.OBSERVABLE
import uk.co.glass_software.android.dejavu.test.*
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse

class AnnotationProcessorUnitTest {

    private val defaultCacheDuration = 4321L
    private val defaultNetworkTimeOut = 1234L
    private val responseKClass = TestResponse::class
    private val responseClass = responseKClass.java

    private lateinit var configuration: CacheConfiguration<Glitch>
    private lateinit var target: AnnotationProcessor<Glitch>

    @Before
    fun setUp() {
        configuration = mock()

        whenever(configuration.logger).thenReturn(mock())
        whenever(configuration.cacheAllByDefault).thenReturn(false)
        whenever(configuration.cacheDurationInMillis).thenReturn(defaultCacheDuration)
        whenever(configuration.connectivityTimeoutInMillis).thenReturn(defaultNetworkTimeOut)
        whenever(configuration.mergeOnNextOnError).thenReturn(true)
        whenever(configuration.encrypt).thenReturn(true)
        whenever(configuration.compress).thenReturn(true)

        target = AnnotationProcessor(configuration)
    }

    @Test
    fun testProcessWithNoAnnotationsCacheAllByDefaultFalse() {
        testProcessWithNoAnnotations(false)
    }

    @Test
    fun testProcessWithNoAnnotationsCacheAllByDefaultTrue() {
        testProcessWithNoAnnotations(true)
    }

    private fun testProcessWithNoAnnotations(cacheAllByDefault: Boolean) {
        whenever(configuration.cacheAllByDefault).thenReturn(cacheAllByDefault)

        val instruction = target.process(
                arrayOf(),
                OBSERVABLE,
                responseClass
        )

        if (cacheAllByDefault) {
            assertInstruction(
                    CacheInstruction.Operation.Expiring.Cache(
                            defaultCacheDuration,
                            defaultNetworkTimeOut,
                            false,
                            true,
                            true,
                            true,
                            false
                    ),
                    instruction,
                    "Instruction invalid when no annotations are present and cacheAllByDefault is true"
            )

            verify(configuration).cacheAllByDefault
            verify(configuration).cacheDurationInMillis
            verify(configuration).connectivityTimeoutInMillis
            verify(configuration).mergeOnNextOnError
            verify(configuration).encrypt
            verify(configuration).compress
        } else {
            assertNullWithContext(
                    instruction,
                    "Instruction should be null when no annotations are present and cacheAllByDefault is false"
            )
        }
    }

    @Test
    fun testProcessWithTwoAnnotations() {
        val expectedErrorMessage = ("More than one cache annotation defined for method returning"
                + " ${OBSERVABLE.getTypedName(responseClass)}, found ${OFFLINE.annotationName}"
                + " after existing annotation ${DO_NOT_CACHE.annotationName}."
                + " Only one annotation can be used for this method.")

        expectException(
                CacheException::class.java,
                expectedErrorMessage,
                {
                    target.process(
                            arrayOf(
                                    getAnnotation<DoNotCache>(emptyList()),
                                    getAnnotation<Offline>(emptyList())
                            ),
                            OBSERVABLE,
                            responseClass
                    )
                }
        )
    }

    @Test
    fun testProcessCacheDefaultDurations() {
        testProcessAnnotation(
                getAnnotation<Cache>(listOf(
                        true,
                        -1L,
                        -1L,
                        OptionalBoolean.TRUE,
                        OptionalBoolean.TRUE,
                        OptionalBoolean.TRUE
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Cache(
                                defaultCacheDuration,
                                defaultNetworkTimeOut,
                                true,
                                true,
                                true,
                                true,
                                false
                        )
                )
        )
    }

    @Test
    fun testProcessCacheDefaultNetworkTimeout() {
        testProcessAnnotation(
                getAnnotation<Cache>(listOf(
                        true,
                        4567L,
                        -1L,
                        OptionalBoolean.TRUE,
                        OptionalBoolean.TRUE,
                        OptionalBoolean.TRUE
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Cache(
                                4567L,
                                defaultNetworkTimeOut,
                                true,
                                true,
                                true,
                                true,
                                false
                        )
                )
        )
    }

    @Test
    fun testProcessCacheNoDefaultDurations() {
        testProcessAnnotation(
                getAnnotation<Cache>(listOf(
                        true,
                        4567L,
                        5678L,
                        OptionalBoolean.TRUE,
                        OptionalBoolean.TRUE,
                        OptionalBoolean.TRUE
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Cache(
                                4567L,
                                5678L,
                                true,
                                true,
                                true,
                                true,
                                false
                        )
                )
        )
    }

    @Test
    fun testProcessRefreshDefaultDurations() {
        testProcessAnnotation(
                getAnnotation<Refresh>(listOf(
                        true,
                        -1L,
                        -1L,
                        OptionalBoolean.TRUE
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Refresh(
                                defaultCacheDuration,
                                defaultNetworkTimeOut,
                                true,
                                true,
                                false
                        )
                )
        )
    }

    @Test
    fun testProcessRefreshDefaultNetworkTimeout() {
        testProcessAnnotation(
                getAnnotation<Refresh>(listOf(
                        true,
                        4567L,
                        -1L,
                        OptionalBoolean.TRUE
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Refresh(
                                4567L,
                                defaultNetworkTimeOut,
                                true,
                                true,
                                false
                        )
                )
        )
    }

    @Test
    fun testProcessRefreshNoDefaultDurations() {
        testProcessAnnotation(
                getAnnotation<Refresh>(listOf(
                        true,
                        4567L,
                        5678L,
                        OptionalBoolean.TRUE
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Refresh(
                                4567L,
                                5678L,
                                true,
                                true,
                                false
                        )
                )
        )
    }

    @Test
    fun testProcessOffline() {
        testProcessAnnotation(
                getAnnotation<Offline>(listOf()),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Offline()
                )
        )
    }

    @Test
    fun testProcessOfflineWithArgs() {
        testProcessAnnotation(
                getAnnotation<Offline>(listOf(
                        true,
                        OptionalBoolean.TRUE
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Offline(
                                true,
                                true
                        )
                )
        )
    }

    @Test
    fun testProcessInvalidate() {
        testProcessAnnotation(
                getAnnotation<Invalidate>(listOf(responseKClass)),
                cacheInstruction(
                        CacheInstruction.Operation.Invalidate
                )
        )
    }

    @Test
    fun testProcessDoNotCache() {
        testProcessAnnotation(
                getAnnotation<DoNotCache>(listOf()),
                cacheInstruction(
                        CacheInstruction.Operation.DoNotCache
                )
        )
    }

    @Test
    fun testProcessClearTargetResponseClass() {
        testProcessAnnotation(
                getAnnotation<Clear>(listOf(
                        responseKClass,
                        false
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Clear(responseClass)
                )
        )
    }

    @Test
    fun testProcessClearTargetResponseClassOlderEntries() {
        testProcessAnnotation(
                getAnnotation<Clear>(listOf(
                        responseKClass,
                        true
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Clear(
                                responseClass,
                                true
                        )
                )
        )
    }

    @Test
    fun testProcessClearAll() {
        testProcessAnnotation(
                getAnnotation<ClearAll>(listOf(false)),
                cacheInstruction(
                        CacheInstruction.Operation.Clear(
                                null,
                                false
                        )
                )
        )
    }

    @Test
    fun testProcessClearAllOlderEntries() {
        testProcessAnnotation(
                getAnnotation<ClearAll>(listOf(true)),
                cacheInstruction(
                        CacheInstruction.Operation.Clear(
                                null,
                                true
                        )
                )
        )
    }

    private fun testProcessAnnotation(annotation: Annotation,
                                      expectedInstruction: CacheInstruction) {
        val actualInstruction = target.process(
                arrayOf(annotation),
                OBSERVABLE,
                responseClass
        )

        assertInstruction(
                expectedInstruction,
                actualInstruction,
                "Failed processing annotation $annotation"
        )
    }

}
