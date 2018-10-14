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

package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
import io.reactivex.Observable
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import uk.co.glass_software.android.cache_interceptor.base.network.model.TestResponse
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database.DatabaseManager
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation.Hasher
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import java.util.*

class DatabaseManagerUnitTest {

    private var mockDb: SQLiteDatabase? = null
    private var mockSerialisationManager: SerialisationManager<*>? = null
    private var mockObservable: Observable<TestResponse>? = null
    private var mockCacheToken: CacheToken? = null
    private var mockCursor: Cursor? = null
    private var mockResponse: TestResponse? = null
    private var mockContentValuesFactory: Function? = null
    private var mockMetadata: ResponseMetadata? = null
    private var cacheKey: String? = null
    private var mockBlob: ByteArray? = null

    private var mockDateProvider: Function? = null
    private val currentDateTime = 239094L
    private var mockCurrentDate: Date? = null
    private val mockFetchDateTime = 182938L
    private var mockFetchDate: Date? = null
    private val mockCacheDateTime = 1234L
    private var mockCacheDate: Date? = null
    private val mockExpiryDateTime = 5678L
    private var mockExpiryDate: Date? = null

    private var target: DatabaseManager<*>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        val mockLogger = mock(Logger::class.java)

        mockDb = mock(SQLiteDatabase::class.java)
        mockObservable = mock<Observable<*>>(Observable<*>::class.java)
        mockSerialisationManager = mock(SerialisationManager<*>::class.java)
        mockDateProvider = mock(Function::class.java)

        mockCurrentDate = mock(Date::class.java)
        mockFetchDate = mock(Date::class.java)
        mockCacheDate = mock(Date::class.java)
        mockExpiryDate = mock(Date::class.java)

        `when`(mockCurrentDate!!.time).thenReturn(currentDateTime)
        `when`(mockFetchDate!!.time).thenReturn(mockFetchDateTime)
        `when`(mockCacheDate!!.time).thenReturn(mockCacheDateTime)
        `when`(mockExpiryDate!!.time).thenReturn(mockExpiryDateTime)

        `when`(mockDateProvider!!.get(eq<T>(null))).thenReturn(mockCurrentDate)
        `when`(mockDateProvider!!.get(eq(mockCacheDateTime))).thenReturn(mockCacheDate)
        `when`(mockDateProvider!!.get(eq(mockExpiryDateTime))).thenReturn(mockExpiryDate)

        mockContentValuesFactory = mock(Function::class.java)

        mockCacheToken = mock(CacheToken::class.java)
        mockCursor = mock(Cursor::class.java)
        mockResponse = mock(TestResponse::class.java)
        cacheKey = "someKey"
        mockBlob = byteArrayOf(1, 2, 3, 4, 5, 6, 8, 9)

        `when`(mockCacheToken!!.getResponseClass()).thenReturn(TestResponse::class.java)
        `when`(mockCacheToken!!.getKey(any<T>(Hasher::class.java))).thenReturn(cacheKey)

        `when`<Date>(mockCacheToken!!.fetchDate).thenReturn(mockFetchDate)
        `when`<Date>(mockCacheToken!!.cacheDate).thenReturn(mockCacheDate)
        `when`<Date>(mockCacheToken!!.expiryDate).thenReturn(mockExpiryDate)

        mockMetadata = mock(ResponseMetadata::class.java)
        `when`<CacheMetadata<ApiError>>(mockResponse!!.metadata).thenReturn(mockMetadata)
        `when`(mockMetadata!!.getCacheToken()).thenReturn(mockCacheToken)

        target = DatabaseManager(
                mockDb,
                mockSerialisationManager,
                mockLogger,
                mockDateProvider,
                mockContentValuesFactory
        )
    }

    @Test
    @Throws(Exception::class)
    fun testFlushCache() {
        target!!.clearCache()

        val captor = ArgumentCaptor.forClass(String::class.java)
        verify<SQLiteDatabase>(mockDb).execSQL(captor.capture())

        assertEquals("Cache flush SQL command was wrong", "DELETE FROM " + Companion.getTABLE_CACHE(), captor.value)
    }

    @Test
    @Throws(Exception::class)
    fun testCache() {
        `when`<ByteArray>(mockSerialisationManager!!.serialise(eq<TestResponse>(mockResponse))).thenReturn(mockBlob)
        `when`(mockContentValuesFactory!!.get(any<T>())).thenReturn(mock<T>(ContentValues::class.java))

        target!!.cache(mockResponse)

        verify<SQLiteDatabase>(mockDb).insertWithOnConflict(
                eq(Companion.getTABLE_CACHE()),
                eq<String>(null),
                any(),
                eq(CONFLICT_REPLACE)
        )

        val mapCaptor = ArgumentCaptor.forClass(Map<*, *>::class.java)
        verify<Any>(mockContentValuesFactory).get(mapCaptor.capture())
        val values = mapCaptor.value

        assertEquals("Cache key didn't match", cacheKey, values[Companion.getCOLUMN_CACHE_TOKEN()])
        assertEquals("Cache date didn't match", mockCacheDateTime, values[Companion.getCOLUMN_CACHE_DATE()])
        assertEquals("Expiry date didn't match", mockExpiryDateTime, values[Companion.getCOLUMN_CACHE_EXPIRY_DATE()])
        assertEquals("Cached data didn't match", mockBlob, values[Companion.getCOLUMN_CACHE_DATA()])
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

        doReturn(mockCursor).`when`<SQLiteDatabase>(mockDb)
                .query(eq(Companion.getTABLE_CACHE()),
                        any(),
                        any(),
                        any(),
                        eq<String>(null),
                        eq<String>(null),
                        eq<String>(null),
                        eq("1")
                )

        if (hasResults) {
            `when`(mockCursor!!.count).thenReturn(1)
            `when`(mockCursor!!.moveToNext()).thenReturn(true)

            `when`(mockCursor!!.getLong(eq(0))).thenReturn(mockCacheDateTime)
            `when`(mockCursor!!.getLong(eq(1))).thenReturn(mockExpiryDateTime)
            `when`(mockCursor!!.getBlob(eq(2))).thenReturn(mockBlob)
            `when`(mockExpiryDate!!.time).thenReturn(mockExpiryDateTime)

            doReturn(mockResponse).`when`<SerialisationManager>(mockSerialisationManager)
                    .deserialise(eq(TestResponse::class.java),
                            eq<ByteArray>(mockBlob),
                            any<Any>()
                    )

            `when`(mockCurrentDate!!.time).thenReturn(mockExpiryDateTime + if (isStale) 1 else -1)

            `when`(mockCacheToken!!.apiUrl).thenReturn(mockUrl)
            `when`(mockCacheToken!!.getResponseClass()).thenReturn(TestResponse::class.java)
        } else {
            `when`(mockCursor!!.count).thenReturn(0)
        }

        val cachedResponse = target!!.getCachedResponse(mockObservable!!, mockCacheToken!!.toLong())

        val projectionCaptor = ArgumentCaptor.forClass(Array<String>::class.java)
        val selectionCaptor = ArgumentCaptor.forClass(String::class.java)
        val selectionArgsCaptor = ArgumentCaptor.forClass(Array<String>::class.java)

        verify<SQLiteDatabase>(mockDb).query(eq(Companion.getTABLE_CACHE()),
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

        assertEquals("Wrong selection", "token = ?", selection)

        assertEquals("Wrong projection size", 3, projection.size)
        assertEquals("Wrong projection at position 0", "cache_date", projection[0])
        assertEquals("Wrong projection at position 1", "expiry_date", projection[1])
        assertEquals("Wrong projection at position 2", "data", projection[2])

        assertEquals("Wrong selection args size", 1, selectionArgs.size)
        assertEquals("Wrong selection arg at position 0", cacheKey, selectionArgs[0])

        if (hasResults) {
            assertEquals("Cached response didn't match", mockResponse, cachedResponse)

            val metadataCaptor = ArgumentCaptor.forClass(ResponseMetadata::class.java)
            verify<TestResponse>(cachedResponse).metadata = metadataCaptor.capture()
            val cacheToken = metadataCaptor.getValue().getCacheToken()

            assertEquals("Cached response class didn't match", TestResponse::class.java, cacheToken.getResponseClass())
            assertEquals("Cache date didn't match", mockCacheDate, cacheToken.cacheDate)
            assertEquals("Expiry date didn't match", mockExpiryDate, cacheToken.expiryDate)
            assertEquals("Refresh observable didn't match", mockObservable, cacheToken.getRefreshObservable())
            assertEquals("Response class didn't match", TestResponse::class.java, cacheToken.getResponseClass())
            assertEquals("Url didn't match", mockUrl, cacheToken.apiUrl)
            assertEquals("Cached response should be CACHED", CacheStatus.CACHED, cacheToken.status)
        } else {
            assertNull("Cached response should be null", cachedResponse)
        }

        verify<Cursor>(mockCursor).close()
    }
}