package uk.co.glass_software.android.dejavu.interceptors.internal.cache.database

import android.content.ContentValues
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Observable
import io.requery.android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import org.junit.Test
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.SqlOpenHelperCallback.Companion.COLUMNS.*
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.SqlOpenHelperCallback.Companion.TABLE_CACHE
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.test.*
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import java.util.*

class DatabaseManagerUnitTest {

    private lateinit var mockDb: SupportSQLiteDatabase
    private lateinit var mockSerialisationManager: SerialisationManager<Glitch>
    private lateinit var mockObservable: Observable<TestResponse>
    private lateinit var mockCacheToken: CacheToken
    private lateinit var mockCursor: Cursor
    private lateinit var mockResponseWrapper: ResponseWrapper<Glitch>
    private lateinit var mockContentValuesFactory: (Map<String, *>) -> ContentValues
    private lateinit var mockDateFactory: (Long?) -> Date
    private lateinit var mockMetadata: CacheMetadata<Glitch>
    private lateinit var cacheKey: String
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

    private lateinit var target: DatabaseManager<Glitch>

    private fun setUp() {
        val mockLogger = mock<Logger>()

        mockDb = mock()
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

        cacheKey = "someKey"
        mockBlob = byteArrayOf(1, 2, 3, 4, 5, 6, 8, 9)

        whenever(mockCacheToken.fetchDate).thenReturn(mockFetchDate)
        whenever(mockCacheToken.cacheDate).thenReturn(mockCacheDate)
        whenever(mockCacheToken.expiryDate).thenReturn(mockExpiryDate)

        whenever(mockResponseWrapper.metadata).thenReturn(mockMetadata)
        whenever(mockMetadata.cacheToken).thenReturn(mockCacheToken)

        target = DatabaseManager(
                mockDb,
                mockSerialisationManager,
                mockLogger,
                true,
                true,
                durationInMillis,
                mockDateFactory,
                mockContentValuesFactory
        )
    }

    @Test
    @Throws(Exception::class)
    fun testClearCache() {
        trueFalseSequence { useTypeToClear ->
            trueFalseSequence { clearOlderEntriesOnly ->
                setUp()
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

        target.clearCache(
                typeToClearClass,
                clearOlderEntriesOnly
        )

        val tableCaptor = argumentCaptor<String>()
        val clauseCaptor = argumentCaptor<String>()
        val valueCaptor = argumentCaptor<Array<Any>>()

        verify(mockDb).delete(
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
                trueFalseSequence { encryptData ->
                    trueFalseSequence { compressData ->
                        trueFalseSequence { hasPreviousResponse ->
                            trueFalseSequence { isSerialisationSuccess ->
                                setUp()
                                testCache(
                                        iteration++,
                                        operation,
                                        encryptData,
                                        compressData,
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
        val mockPreviousResponse = if (hasPreviousResponse) mock<ResponseWrapper<Glitch>>() else null

        val duration = operation.durationInMillis ?: durationInMillis

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


        val previousMetadata = CacheMetadata<Glitch>(
                instructionToken(),
                null,
                CacheMetadata.Duration(0, 0, 0)
        )

        if (mockPreviousResponse != null) {
            whenever(mockPreviousResponse.metadata).thenReturn(previousMetadata)
        }

        target.cache(
                instructionToken,
                operation,
                mockResponseWrapper,
                mockPreviousResponse
        ).blockingGet()

        if (isSerialisationSuccess) {
            verifyWithContext(mockDb, context).insert(
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
            verifyNeverWithContext(mockDb, context).insert(
                    any(),
                    any(),
                    any()
            )
        }
    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testGetCachedResponseWithStaleResult() {
//        testGetCachedResponse(true, true)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testGetCachedResponseWithFreshResult() {
//        testGetCachedResponse(true, false)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testGetCachedResponseNoResult() {
//        testGetCachedResponse(false, false)
//    }
//
//    private fun testGetCachedResponse(hasResults: Boolean,
//                                      isStale: Boolean) {
//        val mockUrl = "mockUrl"
//
//        doReturn(mockCursor).`when`(mockDb)
//                .query(eq(TABLE_CACHE),
//                        any(),
//                        any(),
//                        any(),
//                        isNull<String>(),
//                        isNull<String>(),
//                        isNull<String>(),
//                        eq("1")
//                )
//
//        if (hasResults) {
//            whenever(mockCursor.count).thenReturn(1)
//            whenever(mockCursor.moveToNext()).thenReturn(true)
//
//            whenever(mockCursor.getLong(eq(0))).thenReturn(mockCacheDateTime)
//            whenever(mockCursor.getLong(eq(1))).thenReturn(mockExpiryDateTime)
//            whenever(mockCursor.getBlob(eq(2))).thenReturn(mockBlob)
//            whenever(mockExpiryDate.time).thenReturn(mockExpiryDateTime)
//
//            doReturn(mockResponse).whenever(mockSerialisationManager)
//                    .deserialise(eq(TestResponse::class.java),
//                            eq(mockBlob),
//                            any()
//                    )
//
//            whenever(mockCurrentDate.time).thenReturn(mockExpiryDateTime + if (isStale) 1 else -1)
//
//            whenever(mockCacheToken.apiUrl).thenReturn(mockUrl)
//            whenever(mockCacheToken.getResponseClass()).thenReturn(TestResponse::class.java)
//        } else {
//            whenever(mockCursor.count).thenReturn(0)
//        }
//
//        val cachedResponse = target.getCachedResponse(mockObservable, mockCacheToken.toLong())
//
//        val projectionCaptor = ArgumentCaptor.forClass(Array<String>::class.java)
//        val selectionCaptor = ArgumentCaptor.forClass(String::class.java)
//        val selectionArgsCaptor = ArgumentCaptor.forClass(Array<String>::class.java)
//
//        verify(mockDb).query(
//                eq(TABLE_CACHE),
//                projectionCaptor.capture(),
//                selectionCaptor.capture(),
//                selectionArgsCaptor.capture(),
//                isNull(),
//                isNull(),
//                isNull(),
//                eq("1")
//        )
//
//        val projection = projectionCaptor.value
//        val selection = selectionCaptor.value
//        val selectionArgs = selectionArgsCaptor.value
//
//        assertEqualsWithContext(
//                "token = ?",
//                selection,
//                "Wrong selection"
//        )
//
//        assertEqualsWithContext(
//                3,
//                projection.size,
//                "Wrong projection size"
//        )
//
//        assertEqualsWithContext(
//                "cache_date",
//                projection[0],
//                "Wrong projection at position 0"
//        )
//        assertEqualsWithContext(
//                "expiry_date",
//                projection[1],
//                "Wrong projection at position 1"
//        )
//        assertEqualsWithContext(
//                "data",
//                projection[2],
//                "Wrong projection at position 2"
//        )
//
//        assertEqualsWithContext(
//                1,
//                selectionArgs.size,
//                "Wrong selection args size"
//        )
//        assertEqualsWithContext(
//                cacheKey,
//                selectionArgs[0],
//                "Wrong selection arg at position 0"
//        )
//
//        if (hasResults) {
//            assertEqualsWithContext(
//                    mockResponse,
//                    cachedResponse,
//                    "Cached response didn't match"
//            )
//
//            val metadataCaptor = argumentCaptor<CacheMetadata<Glitch>>()
//            verify(cachedResponse).metadata = metadataCaptor.capture()
//            val cacheToken = metadataCaptor.firstValue.cacheToken
//
//            assertEqualsWithContext(
//                    TestResponse::class.java,
//                    cacheToken.responseClass,
//                    "Cached response class didn't match"
//            )
//
//            assertEqualsWithContext(
//                    mockCacheDate,
//                    cacheToken.cacheDate,
//                    "Cache date didn't match"
//            )
//
//            assertEqualsWithContext(
//                    "Expiry date didn't match",
//                    mockExpiryDate, cacheToken.expiryDate)
//
//            assertEqualsWithContext(
//                    TestResponse::class.java,
//                    cacheToken.getResponseClass(),
//                    "Response class didn't match"
//            )
//
//            assertEqualsWithContext(
//                    mockUrl,
//                    cacheToken.apiUrl,
//                    "Url didn't match"
//            )
//
//            assertEqualsWithContext(
//                    CacheStatus.CACHED,
//                    cacheToken.status,
//                    "Cached response should be CACHED"
//            )
//        } else {
//            assertNullWithContext(
//                    cachedResponse,
//                    "Cached response should be null"
//            )
//        }
//
//        verify(mockCursor).close()
//    }
}