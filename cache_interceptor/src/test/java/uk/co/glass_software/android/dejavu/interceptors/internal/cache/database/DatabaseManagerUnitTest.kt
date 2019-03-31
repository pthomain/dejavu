package uk.co.glass_software.android.dejavu.interceptors.internal.cache.database

import android.content.ContentValues
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Observable
import io.requery.android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import org.junit.Test
import uk.co.glass_software.android.boilerplate.utils.lambda.Action
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Type.INVALIDATE
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Type.REFRESH
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.SqlOpenHelperCallback.Companion.COLUMNS.*
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.SqlOpenHelperCallback.Companion.TABLE_CACHE
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.test.*
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import java.util.*

class DatabaseManagerUnitTest {

    private lateinit var mockDatabase: SupportSQLiteDatabase
    private lateinit var mockSerialisationManager: SerialisationManager<Glitch>
    private lateinit var mockObservable: Observable<TestResponse>
    private lateinit var mockCacheToken: CacheToken
    private lateinit var mockCursor: Cursor
    private lateinit var mockResponseWrapper: ResponseWrapper<Glitch>
    private lateinit var mockContentValuesFactory: (Map<String, *>) -> ContentValues
    private lateinit var mockDateFactory: (Long?) -> Date
    private lateinit var mockMetadata: CacheMetadata<Glitch>
    private lateinit var mockBlob: ByteArray

    private val currentDateTime = 10000L
    private val mockFetchDateTime = 1000L
    private val mockCacheDateTime = 100L
    private val mockExpiryDateTime = 10L
    private val durationInMillis = 5L

    private val mockCurrentDate = Date(currentDateTime)
    private val mockFetchDate = Date(mockFetchDateTime)
    private val mockCacheDate = Date(mockCacheDateTime)
    private val mockExpiryDate = Date(mockExpiryDateTime)

    private fun setUp(encryptDataGlobally: Boolean,
                      compressDataGlobally: Boolean,
                      cacheInstruction: CacheInstruction?): DatabaseManager<Glitch> {
        mockDatabase = mock()
        mockObservable = mock()
        mockSerialisationManager = mock()
        mockDateFactory = mock()

        whenever(mockDateFactory.invoke(isNull())).thenReturn(mockCurrentDate)
        whenever(mockDateFactory.invoke(eq(mockCacheDateTime))).thenReturn(mockCacheDate)
        whenever(mockDateFactory.invoke(eq(mockExpiryDateTime))).thenReturn(mockExpiryDate)

        mockContentValuesFactory = mock()
        mockCacheToken = mock()
        mockCursor = mock()
        mockResponseWrapper = mock()
        mockMetadata = mock()

        mockBlob = byteArrayOf(1, 2, 3, 4, 5, 6, 8, 9)

        whenever(mockCacheToken.fetchDate).thenReturn(mockFetchDate)
        whenever(mockCacheToken.cacheDate).thenReturn(mockCacheDate)
        whenever(mockCacheToken.expiryDate).thenReturn(mockExpiryDate)

        whenever(mockResponseWrapper.metadata).thenReturn(mockMetadata)
        whenever(mockMetadata.cacheToken).thenReturn(mockCacheToken)

        if (cacheInstruction != null) {
            whenever(mockCacheToken.instruction).thenReturn(cacheInstruction)
        }

        return DatabaseManager(
                mockDatabase,
                mockSerialisationManager,
                mock(),
                compressDataGlobally,
                encryptDataGlobally,
                durationInMillis,
                mockDateFactory,
                mockContentValuesFactory
        )
    }

    @Test
    fun testClearCache() {
        trueFalseSequence { useTypeToClear ->
            trueFalseSequence { clearOlderEntriesOnly ->
                testClearCache(
                        useTypeToClear,
                        clearOlderEntriesOnly
                )
            }
        }
    }

    private fun testClearCache(useTypeToClear: Boolean,
                               clearOlderEntriesOnly: Boolean) {
        val context = "useTypeToClear = $useTypeToClear\nclearOlderEntriesOnly = $clearOlderEntriesOnly"

        val typeToClearClass: Class<*>? = if (useTypeToClear) TestResponse::class.java else null

        val target = setUp(
                true,
                true,
                null
        )

        target.clearCache(
                typeToClearClass,
                clearOlderEntriesOnly
        )

        val tableCaptor = argumentCaptor<String>()
        val clauseCaptor = argumentCaptor<String>()
        val valueCaptor = argumentCaptor<Array<Any>>()

        verify(mockDatabase).delete(
                tableCaptor.capture(),
                clauseCaptor.capture(),
                valueCaptor.capture()
        )

        val expectedClause = when {
            useTypeToClear -> if (clearOlderEntriesOnly) "expiry_date < ? AND class = ?" else "class = ?"
            else -> if (clearOlderEntriesOnly) "expiry_date < ?" else ""
        }

        val responseType = TestResponse::class.java.name

        val expectedValue = when {
            useTypeToClear -> if (clearOlderEntriesOnly) arrayOf(mockCurrentDate.time.toString(), responseType) else arrayOf(responseType)
            else -> if (clearOlderEntriesOnly) arrayOf(mockCurrentDate.time.toString()) else emptyArray()
        }

        assertEqualsWithContext(
                SqlOpenHelperCallback.TABLE_CACHE,
                tableCaptor.firstValue,
                "Clear cache target table didn't match",
                context
        )

        assertEqualsWithContext(
                expectedClause,
                clauseCaptor.firstValue,
                "Clear cache clause didn't match",
                context
        )

        assertEqualsWithContext(
                expectedValue,
                valueCaptor.firstValue,
                "Clear cache clause values didn't match",
                context
        )
    }

    @Test
    fun testCache() {
        var iteration = 0
        operationSequence { operation ->
            if (operation is Expiring) {
                trueFalseSequence { encryptDataGlobally ->
                    trueFalseSequence { compressDataGlobally ->
                        trueFalseSequence { hasPreviousResponse ->
                            trueFalseSequence { isSerialisationSuccess ->
                                testCache(
                                        iteration++,
                                        operation,
                                        encryptDataGlobally,
                                        compressDataGlobally,
                                        hasPreviousResponse,
                                        isSerialisationSuccess
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun testCache(iteration: Int,
                          operation: Expiring,
                          encryptDataGlobally: Boolean,
                          compressDataGlobally: Boolean,
                          hasPreviousResponse: Boolean,
                          isSerialisationSuccess: Boolean) {

        val context = "iteration = $iteration,\n" +
                "operation = $operation,\n" +
                "encryptDataGlobally = $encryptDataGlobally,\n" +
                "compressDataGlobally = $compressDataGlobally,\n" +
                "hasPreviousResponse = $hasPreviousResponse\n" +
                "isSerialisationSuccess = $isSerialisationSuccess"

        val instructionToken = instructionToken(operation)

        val target = setUp(
                encryptDataGlobally,
                compressDataGlobally,
                instructionToken.instruction
        )

        whenever(mockResponseWrapper.metadata.cacheToken.requestMetadata).thenReturn(instructionToken.requestMetadata)
        val mockPreviousResponse = if (hasPreviousResponse) mock<ResponseWrapper<Glitch>>() else null

        val duration = operation.durationInMillis ?: durationInMillis

        if (mockPreviousResponse != null) {
            val previousMetadata = CacheMetadata<Glitch>(
                    instructionToken(),
                    null,
                    CacheMetadata.Duration(0, 0, 0)
            )
            whenever(mockPreviousResponse.metadata).thenReturn(previousMetadata)
        }

        val encryptData = mockPreviousResponse?.metadata?.cacheToken?.isEncrypted
                ?: operation.encrypt
                ?: encryptDataGlobally

        val compressData = mockPreviousResponse?.metadata?.cacheToken?.isCompressed
                ?: operation.compress
                ?: compressDataGlobally

        whenever(mockSerialisationManager.serialise(
                eq(mockResponseWrapper),
                eq(encryptData),
                eq(compressData)
        )).thenReturn(if (isSerialisationSuccess) mockBlob else null)

        val mockContentValues = mock<ContentValues>()
        whenever(mockContentValuesFactory.invoke(any())).thenReturn(mockContentValues)

        target.cache(
                mockResponseWrapper,
                mockPreviousResponse
        ).blockingGet()

        if (isSerialisationSuccess) {
            verifyWithContext(mockDatabase, context).insert(
                    eq(TABLE_CACHE),
                    eq(CONFLICT_REPLACE),
                    eq(mockContentValues)
            )

            val mapCaptor = argumentCaptor<Map<String, *>>()
            verify(mockContentValuesFactory).invoke(mapCaptor.capture())

            val values = mapCaptor.firstValue

            assertEqualsWithContext(
                    instructionToken.requestMetadata.hash,
                    values[TOKEN.columnName],
                    "Cache key didn't match",
                    context
            )
            assertEqualsWithContext(
                    currentDateTime,
                    values[DATE.columnName],
                    "Cache date didn't match",
                    context
            )
            assertEqualsWithContext(
                    currentDateTime + duration,
                    values[EXPIRY_DATE.columnName],
                    "Expiry date didn't match",
                    context
            )
            assertEqualsWithContext(
                    mockBlob,
                    values[DATA.columnName],
                    "Cached data didn't match",
                    context
            )
            assertEqualsWithContext(
                    TestResponse::class.java.name,
                    values[CLASS.columnName],
                    "Cached data response class didn't match",
                    context
            )
            assertEqualsWithContext(
                    if (compressData) 1 else 0,
                    values[IS_COMPRESSED.columnName],
                    "Compress data flag didn't match",
                    context
            )
            assertEqualsWithContext(
                    if (encryptData) 1 else 0,
                    values[IS_ENCRYPTED.columnName],
                    "Encrypt data flag didn't match",
                    context
            )
        } else {
            verifyNeverWithContext(mockDatabase, context).insert(
                    any(),
                    any(),
                    any()
            )
        }
    }

    @Test
    fun testInvalidate() {
        operationSequence { operation ->
            testInvalidate(operation)
        }
    }

    private fun testInvalidate(operation: CacheInstruction.Operation) {
        val context = "operation = $operation"

        val target = setUp(
                true,
                true,
                null
        )

        if (operation.type == INVALIDATE || operation.type == REFRESH) {

            val mockContentValues = mock<ContentValues>()
            whenever(mockContentValuesFactory.invoke(any())).thenReturn(mockContentValues)

            val mapCaptor = argumentCaptor<Map<String, Any>>()
            val tableCaptor = argumentCaptor<String>()
            val conflictCaptor = argumentCaptor<Int>()
            val selectionCaptor = argumentCaptor<String>()
            val selectionArgsCaptor = argumentCaptor<Array<String>>()

            val instructionToken = instructionToken(operation)

            target.invalidate(instructionToken)

            verifyWithContext(mockContentValuesFactory, context).invoke(mapCaptor.capture())

            verifyWithContext(mockDatabase, context).update(
                    tableCaptor.capture(),
                    conflictCaptor.capture(),
                    eq(mockContentValues),
                    selectionCaptor.capture(),
                    selectionArgsCaptor.capture()
            )

            assertEqualsWithContext(
                    TABLE_CACHE,
                    tableCaptor.firstValue,
                    "Table value didn't match",
                    context
            )

            assertEqualsWithContext(
                    CONFLICT_REPLACE,
                    conflictCaptor.firstValue,
                    "Conflict flag value didn't match",
                    context
            )

            assertEqualsWithContext(
                    "${TOKEN.columnName} = ?",
                    selectionCaptor.firstValue,
                    "Selection didn't match",
                    context
            )

            assertEqualsWithContext(
                    arrayOf(instructionToken.requestMetadata.hash),
                    selectionArgsCaptor.firstValue,
                    "Selection args didn't match",
                    context
            )

            assertEqualsWithContext(
                    mapOf(EXPIRY_DATE.columnName to 0),
                    mapCaptor.firstValue,
                    "Content values didn't match",
                    context
            )
        } else {
            verifyNeverWithContext(mockDatabase, context).update(
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
            )
        }
    }

    @Test
    fun testGetCachedResponse() {
        var iteration = 0
        operationSequence { operation ->
            if (operation is Expiring) {
                trueFalseSequence { hasResults ->
                    trueFalseSequence { isStale ->
                        testGetCachedResponse(
                                iteration++,
                                operation,
                                hasResults,
                                isStale
                        )
                    }
                }
            }
        }
    }

    private fun testGetCachedResponse(iteration: Int,
                                      operation: Expiring,
                                      hasResponse: Boolean,
                                      isStale: Boolean) {
        val context = "iteration = $iteration,\n" +
                "operation = $operation,\n" +
                "hasResponse = $hasResponse,\n" +
                "isStale = $isStale"

        val target = spy(setUp(
                true,
                true,
                instructionToken(operation).instruction
        ))

        val start = 1234L
        val instructionToken = instructionToken(operation)
        val isDataStale = isStale || operation.type == REFRESH || operation.type == INVALIDATE

        val queryCaptor = argumentCaptor<String>()

        val mockCursor = mock<Cursor>()
        whenever(mockDatabase.query(any<String>())).thenReturn(mockCursor)

        whenever(mockCursor.count).thenReturn(1)

        whenever(mockCursor.moveToNext())
                .thenReturn(true)
                .thenReturn(false)

        val cacheDateTimeStamp = 98765L
        val localData = byteArrayOf(1, 2, 3, 4)
        val isCompressed = 1
        val isEncrypted = 0
        val expiryDate = if (isDataStale) currentDateTime - 1L else currentDateTime + 1L

        whenever(mockCursor.getColumnIndex(eq(DATE.columnName))).thenReturn(1)
        whenever(mockCursor.getColumnIndex(eq(DATA.columnName))).thenReturn(2)
        whenever(mockCursor.getColumnIndex(eq(IS_COMPRESSED.columnName))).thenReturn(3)
        whenever(mockCursor.getColumnIndex(eq(IS_ENCRYPTED.columnName))).thenReturn(4)
        whenever(mockCursor.getColumnIndex(eq(EXPIRY_DATE.columnName))).thenReturn(5)

        whenever(mockCursor.getLong(eq(1))).thenReturn(cacheDateTimeStamp)
        whenever(mockCursor.getBlob(eq(2))).thenReturn(localData)
        whenever(mockCursor.getInt(eq(3))).thenReturn(isCompressed)
        whenever(mockCursor.getInt(eq(4))).thenReturn(isEncrypted)
        whenever(mockCursor.getLong(eq(5))).thenReturn(expiryDate)

        val mockExpiryDate = Date(if (isDataStale) currentDateTime - 1L else currentDateTime + 1L)

        whenever(mockDateFactory.invoke(eq(cacheDateTimeStamp))).thenReturn(mockCacheDate)
        whenever(mockDateFactory.invoke(eq(expiryDate))).thenReturn(mockExpiryDate)

        val mockResponseWrapper = ResponseWrapper<Glitch>(
                TestResponse::class.java,
                null,
                mock()
        )

        val onErrorCaptor = argumentCaptor<Action>()

        whenever(mockSerialisationManager.deserialise(
                eq(instructionToken),
                eq(localData),
                eq(isEncrypted == 1),
                eq(isCompressed == 1),
                any()
        )).thenReturn(if (hasResponse) mockResponseWrapper else null)

        val actualResponseWrapper = target.getCachedResponse(
                instructionToken,
                start
        )

        verifyWithContext(target, context).checkInvalidation(
                eq(instructionToken.instruction),
                eq(instructionToken.requestMetadata.hash)
        )

        verifyWithContext(mockDatabase, context).query(queryCaptor.capture())

        assertEqualsWithContext(
                "\n" +
                        "            SELECT cache_date, expiry_date, data, is_compressed, is_encrypted\n" +
                        "            FROM rx_cache\n" +
                        "            WHERE token = 'no_hash'\n" +
                        "            LIMIT 1\n" +
                        "            ",
                queryCaptor.firstValue.replace("\\s+", " "),
                "Query didn't match",
                context
        )

        verifyWithContext(mockSerialisationManager, context).deserialise(
                eq(instructionToken),
                eq(localData),
                eq(isEncrypted == 1),
                eq(isCompressed == 1),
                onErrorCaptor.capture()
        )

        if (hasResponse) {
            val actualMetadata = actualResponseWrapper!!.metadata

            assertEqualsWithContext(
                    CacheMetadata.Duration((currentDateTime - start).toInt(), 0, 0),
                    actualMetadata.callDuration,
                    "Metadata call duration didn't match",
                    context
            )

            assertEqualsWithContext(
                    if (isDataStale) CacheStatus.STALE else CacheStatus.CACHED,
                    actualMetadata.cacheToken.status,
                    "Cache status should be ${if (isDataStale) "STALE" else "CACHED"}",
                    context
            )
        } else {
            assertNullWithContext(
                    actualResponseWrapper,
                    "Returned response should be null",
                    context
            )
        }

        onErrorCaptor.firstValue()

        verifyWithContext(target, context).clearCache(
                isNull(),
                eq(false)
        )
    }

}