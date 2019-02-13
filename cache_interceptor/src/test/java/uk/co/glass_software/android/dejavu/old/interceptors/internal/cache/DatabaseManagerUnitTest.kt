/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.dejavu.old.interceptors.internal.cache

import android.content.ContentValues
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nhaarman.mockitokotlin2.*
import io.reactivex.Observable
import io.requery.android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.DatabaseManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.SqlOpenHelperCallback.Companion.TABLE_CACHE
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.test.assertEqualsWithContext
import uk.co.glass_software.android.dejavu.test.assertNullWithContext
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import java.util.*

class DatabaseManagerUnitTest {

    private lateinit var mockDb: SupportSQLiteDatabase
    private lateinit var mockSerialisationManager: SerialisationManager<*>
    private lateinit var mockObservable: Observable<TestResponse>
    private lateinit var mockCacheToken: CacheToken
    private lateinit var mockCursor: Cursor
    private lateinit var mockResponse: TestResponse
    private lateinit var mockContentValuesFactory: (Map<String, *>) -> ContentValues
    private lateinit var mockDateFactory: (Long?) -> Date
    private lateinit var mockMetadata: CacheMetadata<Glitch>
    private lateinit var cacheKey: String
    private lateinit var mockBlob: ByteArray

    private val currentDateTime = 239094L
    private val mockFetchDateTime = 182938L
    private val mockCacheDateTime = 1234L
    private val mockExpiryDateTime = 5678L

    private lateinit var mockCurrentDate: Date
    private lateinit var mockFetchDate: Date
    private lateinit var mockCacheDate: Date
    private lateinit var mockExpiryDate: Date

    private lateinit var target: DatabaseManager<Glitch>

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val mockLogger = mock<Logger>()

        mockDb = mock()
        mockObservable = mock()
        mockSerialisationManager = mock()
        mockDateFactory = mock()
        mockCurrentDate = mock()
        mockFetchDate = mock()
        mockCacheDate = mock()
        mockExpiryDate = mock()

        whenever(mockCurrentDate.time).thenReturn(currentDateTime)
        whenever(mockFetchDate.time).thenReturn(mockFetchDateTime)
        whenever(mockCacheDate.time).thenReturn(mockCacheDateTime)
        whenever(mockExpiryDate.time).thenReturn(mockExpiryDateTime)

        whenever(mockDateFactory.invoke(isNull())).thenReturn(mockCurrentDate)
        whenever(mockDateFactory.invoke(eq(mockCacheDateTime))).thenReturn(mockCacheDate)
        whenever(mockDateFactory.invoke(eq(mockExpiryDateTime))).thenReturn(mockExpiryDate)

        mockContentValuesFactory = mock()
        mockCacheToken = mock()
        mockCursor = mock()
        mockResponse = mock()
        mockMetadata = mock()

        cacheKey = "someKey"
        mockBlob = byteArrayOf(1, 2, 3, 4, 5, 6, 8, 9)

        whenever(mockCacheToken.fetchDate).thenReturn(mockFetchDate)
        whenever(mockCacheToken.cacheDate).thenReturn(mockCacheDate)
        whenever(mockCacheToken.expiryDate).thenReturn(mockExpiryDate)

        whenever(mockResponse.metadata).thenReturn(mockMetadata)
        whenever(mockMetadata.cacheToken).thenReturn(mockCacheToken)

        target = DatabaseManager(
                mockDb,
                mockSerialisationManager,
                mockLogger,
                true,
                true,
                5678,
                mockDateFactory,
                mockContentValuesFactory
        )
    }

    @Test
    @Throws(Exception::class)
    fun testFlushCache() {
        target.clearCache()

        val captor = argumentCaptor<String>()
        verify(mockDb).execSQL(captor.capture())

        assertEqualsWithContext(
                "DELETE FROM $TABLE_CACHE",
                captor.firstValue,
                "Cache flush SQL command was wrong"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testCache() {
        whenever(mockSerialisationManager.serialise(eq(mockResponse))).thenReturn(mockBlob)
        whenever(mockContentValuesFactory.invoke(any())).thenReturn(mock(ContentValues::class.java))

        target.cache(mockResponse)

        verify(mockDb).insertWithOnConflict(
                eq(TABLE_CACHE),
                isNull<String>(),
                any(),
                eq(CONFLICT_REPLACE)
        )

        val mapCaptor = argumentCaptor<Map<*, *>>()
        verify(mockContentValuesFactory).invoke(mapCaptor.capture())
        val values = mapCaptor.firstValue

        assertEqualsWithContext(
                cacheKey,
                values[COLUMN_CACHE_TOKEN],
                "Cache key didn't match"
        )
        assertEqualsWithContext(
                mockCacheDateTime,
                values[COLUMN_CACHE_DATE],
                "Cache date didn't match"
        )
        assertEqualsWithContext(
                mockExpiryDateTime,
                values[COLUMN_CACHE_EXPIRY_DATE],
                "Expiry date didn't match"
        )
        assertEqualsWithContext(
                mockBlob,
                values[COLUMN_CACHE_DATA],
                "Cached data didn't match"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetCachedResponseWithStaleResult() {
        testGetCachedResponse(true, true)
    }

    @Test
    @Throws(Exception::class)
    fun testGetCachedResponseWithFreshResult() {
        testGetCachedResponse(true, false)
    }

    @Test
    @Throws(Exception::class)
    fun testGetCachedResponseNoResult() {
        testGetCachedResponse(false, false)
    }

    private fun testGetCachedResponse(hasResults: Boolean,
                                      isStale: Boolean) {
        val mockUrl = "mockUrl"

        doReturn(mockCursor).`when`(mockDb)
                .query(eq(TABLE_CACHE),
                        any(),
                        any(),
                        any(),
                        isNull<String>(),
                        isNull<String>(),
                        isNull<String>(),
                        eq("1")
                )

        if (hasResults) {
            whenever(mockCursor.count).thenReturn(1)
            whenever(mockCursor.moveToNext()).thenReturn(true)

            whenever(mockCursor.getLong(eq(0))).thenReturn(mockCacheDateTime)
            whenever(mockCursor.getLong(eq(1))).thenReturn(mockExpiryDateTime)
            whenever(mockCursor.getBlob(eq(2))).thenReturn(mockBlob)
            whenever(mockExpiryDate.time).thenReturn(mockExpiryDateTime)

            doReturn(mockResponse).whenever(mockSerialisationManager)
                    .deserialise(eq(TestResponse::class.java),
                            eq(mockBlob),
                            any()
                    )

            whenever(mockCurrentDate.time).thenReturn(mockExpiryDateTime + if (isStale) 1 else -1)

            whenever(mockCacheToken.apiUrl).thenReturn(mockUrl)
            whenever(mockCacheToken.getResponseClass()).thenReturn(TestResponse::class.java)
        } else {
            whenever(mockCursor.count).thenReturn(0)
        }

        val cachedResponse = target.getCachedResponse(mockObservable, mockCacheToken.toLong())

        val projectionCaptor = ArgumentCaptor.forClass(Array<String>::class.java)
        val selectionCaptor = ArgumentCaptor.forClass(String::class.java)
        val selectionArgsCaptor = ArgumentCaptor.forClass(Array<String>::class.java)

        verify(mockDb).query(
                eq(TABLE_CACHE),
                projectionCaptor.capture(),
                selectionCaptor.capture(),
                selectionArgsCaptor.capture(),
                isNull(),
                isNull(),
                isNull(),
                eq("1")
        )

        val projection = projectionCaptor.value
        val selection = selectionCaptor.value
        val selectionArgs = selectionArgsCaptor.value

        assertEqualsWithContext(
                "token = ?",
                selection,
                "Wrong selection"
        )

        assertEqualsWithContext(
                3,
                projection.size,
                "Wrong projection size"
        )

        assertEqualsWithContext(
                "cache_date",
                projection[0],
                "Wrong projection at position 0"
        )
        assertEqualsWithContext(
                "expiry_date",
                projection[1],
                "Wrong projection at position 1"
        )
        assertEqualsWithContext(
                "data",
                projection[2],
                "Wrong projection at position 2"
        )

        assertEqualsWithContext(
                1,
                selectionArgs.size,
                "Wrong selection args size"
        )
        assertEqualsWithContext(
                cacheKey,
                selectionArgs[0],
                "Wrong selection arg at position 0"
        )

        if (hasResults) {
            assertEqualsWithContext(
                    mockResponse,
                    cachedResponse,
                    "Cached response didn't match"
            )

            val metadataCaptor = argumentCaptor<CacheMetadata<Glitch>>()
            verify(cachedResponse).metadata = metadataCaptor.capture()
            val cacheToken = metadataCaptor.firstValue.cacheToken

            assertEqualsWithContext(
                    TestResponse::class.java,
                    cacheToken.responseClass,
                    "Cached response class didn't match"
            )

            assertEqualsWithContext(
                    mockCacheDate,
                    cacheToken.cacheDate,
                    "Cache date didn't match"
            )

            assertEqualsWithContext(
                    "Expiry date didn't match",
                    mockExpiryDate, cacheToken.expiryDate)

            assertEqualsWithContext(
                    TestResponse::class.java,
                    cacheToken.getResponseClass(),
                    "Response class didn't match"
            )

            assertEqualsWithContext(
                    mockUrl,
                    cacheToken.apiUrl,
                    "Url didn't match"
            )

            assertEqualsWithContext(
                    CacheStatus.CACHED,
                    cacheToken.status,
                    "Cached response should be CACHED"
            )
        } else {
            assertNullWithContext(
                    cachedResponse,
                    "Cached response should be null"
            )
        }

        verify(mockCursor).close()
    }
}