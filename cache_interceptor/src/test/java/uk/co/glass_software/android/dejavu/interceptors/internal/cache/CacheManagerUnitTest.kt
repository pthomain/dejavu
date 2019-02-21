package uk.co.glass_software.android.dejavu.interceptors.internal.cache

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import uk.co.glass_software.android.dejavu.configuration.ErrorFactory
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.DatabaseManager
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.interceptors.internal.response.EmptyResponseFactory
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.test.assertEqualsWithContext
import uk.co.glass_software.android.dejavu.test.instructionToken
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import uk.co.glass_software.android.dejavu.test.trueFalseSequence
import uk.co.glass_software.android.dejavu.test.verifyWithContext
import uk.co.glass_software.android.shared_preferences.persistence.serialisation.Serialiser
import java.util.*

class CacheManagerUnitTest {

    //TODO

    private lateinit var mockErrorFactory: ErrorFactory<Glitch>
    private lateinit var mockSerialiser: Serialiser
    private lateinit var mockDatabaseManager: DatabaseManager<Glitch>
    private lateinit var mockEmptyResponseFactory: EmptyResponseFactory<Glitch>
    private lateinit var mockDateFactory: (Long?) -> Date

    private lateinit var target: CacheManager<Glitch>

    @Before
    fun setUp() {
        mockErrorFactory = mock()
        mockSerialiser = mock()
        mockDatabaseManager = mock()
        mockEmptyResponseFactory = mock()
        mockDateFactory = mock()

        target = CacheManager(
                mockErrorFactory,
                mockSerialiser,
                mockDatabaseManager,
                mockEmptyResponseFactory,
                mockDateFactory,
                1000L,
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


    }
}