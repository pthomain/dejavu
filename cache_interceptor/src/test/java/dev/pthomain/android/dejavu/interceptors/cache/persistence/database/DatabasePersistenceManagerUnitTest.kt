/*
 *
 *  Copyright (C) 2017 Pierre Thomain
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package dev.pthomain.android.dejavu.interceptors.cache.persistence.database

import android.content.ContentValues
import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nhaarman.mockitokotlin2.*
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction
import dev.pthomain.android.dejavu.configuration.instruction.Operation
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Expiring
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Type.INVALIDATE
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Type.REFRESH
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.persistence.BasePersistenceManagerUnitTest
import dev.pthomain.android.dejavu.interceptors.cache.persistence.database.SqlOpenHelperCallback.Companion.COLUMNS.*
import dev.pthomain.android.dejavu.interceptors.cache.persistence.database.SqlOpenHelperCallback.Companion.TABLE_DEJA_VU
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager.Factory.Type.DATABASE
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.test.verifyNeverWithContext
import dev.pthomain.android.dejavu.test.verifyWithContext
import io.reactivex.Observable
import io.requery.android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE

internal class DatabasePersistenceManagerUnitTest : BasePersistenceManagerUnitTest<DatabasePersistenceManager<Glitch>>() {

    private lateinit var mockDatabase: SupportSQLiteDatabase
    private lateinit var mockObservable: Observable<TestResponse>
    private lateinit var mockCursor: Cursor
    private lateinit var mockContentValuesFactory: (Map<String, *>) -> ContentValues
    private lateinit var mockContentValues: ContentValues

    override fun setUp(instructionToken: CacheToken,
                       encryptDataGlobally: Boolean,
                       compressDataGlobally: Boolean,
                       cacheInstruction: CacheInstruction?): DatabasePersistenceManager<Glitch> {
        mockDatabase = mock()
        mockObservable = mock()
        mockContentValuesFactory = mock()
        mockCursor = mock()
        mockContentValuesFactory = mock()
        mockContentValues = mock()

        val mockConfiguration = setUpConfiguration(
                encryptDataGlobally,
                compressDataGlobally,
                cacheInstruction
        )

        whenever(mockSerialisationManagerFactory.create(eq(DATABASE))).thenReturn(mockSerialisationManager)


        return DatabasePersistenceManager(
                mockDatabase,
                mockHasher,
                mockSerialisationManager,
                mockConfiguration,
                mockDateFactory,
                mockContentValuesFactory
        )
    }

    override fun verifyClearCache(context: String,
                                  useTypeToClear: Boolean,
                                  clearStaleEntriesOnly: Boolean,
                                  mockClassHash: String) {
        val tableCaptor = argumentCaptor<String>()
        val clauseCaptor = argumentCaptor<String>()
        val valueCaptor = argumentCaptor<Array<Any>>()

        verify(mockDatabase).delete(
                tableCaptor.capture(),
                clauseCaptor.capture(),
                valueCaptor.capture()
        )

        val expectedClause = when {
            useTypeToClear -> if (clearStaleEntriesOnly) "expiry_date < ? AND class = ?" else "class = ?"
            else -> if (clearStaleEntriesOnly) "expiry_date < ?" else ""
        }

        val expectedValue = when {
            useTypeToClear -> ifElse(
                    clearStaleEntriesOnly,
                    arrayOf(mockCurrentDate.time.toString(), mockClassHash),
                    arrayOf(mockClassHash)
            )

            else -> ifElse(
                    clearStaleEntriesOnly,
                    arrayOf(mockCurrentDate.time.toString()),
                    emptyArray()
            )
        }

        assertEqualsWithContext(
                TABLE_DEJA_VU,
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

    override fun prepareCache(iteration: Int,
                              operation: Expiring,
                              encryptDataGlobally: Boolean,
                              compressDataGlobally: Boolean,
                              hasPreviousResponse: Boolean,
                              isSerialisationSuccess: Boolean) {
        whenever(mockContentValuesFactory.invoke(any())).thenReturn(mockContentValues)
    }

    override fun verifyCache(context: String,
                             iteration: Int,
                             instructionToken: CacheToken,
                             operation: Expiring,
                             encryptData: Boolean,
                             compressData: Boolean,
                             hasPreviousResponse: Boolean,
                             isSerialisationSuccess: Boolean,
                             duration: Long) {
        if (isSerialisationSuccess) {
            verifyWithContext(mockDatabase, context).insert(
                    eq(TABLE_DEJA_VU),
                    eq(CONFLICT_REPLACE),
                    eq(mockContentValues)
            )

            val mapCaptor = argumentCaptor<Map<String, *>>()
            verify(mockContentValuesFactory).invoke(mapCaptor.capture())

            val values = mapCaptor.firstValue

            assertEqualsWithContext(
                    instructionToken.instruction.requestMetadata.urlHash,
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
                    instructionToken.instruction.requestMetadata.classHash,
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

    override fun prepareInvalidate(context: String,
                                   operation: Operation,
                                   instructionToken: CacheToken) {
        whenever(mockContentValuesFactory.invoke(any()))
                .thenReturn(mockContentValues)
    }

    override fun prepareCheckInvalidation(context: String,
                                          operation: Operation,
                                          instructionToken: CacheToken) {
        prepareInvalidate(
                context,
                operation,
                instructionToken
        )
    }

    override fun verifyCheckInvalidation(context: String,
                                         operation: Operation,
                                         instructionToken: CacheToken) {
        if (operation.type == INVALIDATE || operation.type == REFRESH) {
            val mapCaptor = argumentCaptor<Map<String, Any>>()
            val tableCaptor = argumentCaptor<String>()
            val conflictCaptor = argumentCaptor<Int>()
            val selectionCaptor = argumentCaptor<String>()
            val selectionArgsCaptor = argumentCaptor<Array<String>>()

            verifyWithContext(mockContentValuesFactory, context).invoke(mapCaptor.capture())

            verifyWithContext(mockDatabase, context).update(
                    tableCaptor.capture(),
                    conflictCaptor.capture(),
                    eq(mockContentValues),
                    selectionCaptor.capture(),
                    selectionArgsCaptor.capture()
            )

            assertEqualsWithContext(
                    TABLE_DEJA_VU,
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
                    arrayOf(instructionToken.instruction.requestMetadata.urlHash),
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

    override fun prepareGetCachedResponse(context: String,
                                          operation: Expiring,
                                          instructionToken: CacheToken,
                                          hasResponse: Boolean,
                                          isStale: Boolean,
                                          isCompressed: Int,
                                          isEncrypted: Int,
                                          cacheDateTimeStamp: Long,
                                          expiryDate: Long) {
        val mockCursor = mock<Cursor>()
        whenever(mockDatabase.query(any<String>())).thenReturn(mockCursor)

        whenever(mockCursor.count).thenReturn(1)

        whenever(mockCursor.moveToNext())
                .thenReturn(true)
                .thenReturn(false)

        whenever(mockCursor.getColumnIndex(eq(DATE.columnName))).thenReturn(1)
        whenever(mockCursor.getColumnIndex(eq(DATA.columnName))).thenReturn(2)
        whenever(mockCursor.getColumnIndex(eq(IS_COMPRESSED.columnName))).thenReturn(3)
        whenever(mockCursor.getColumnIndex(eq(IS_ENCRYPTED.columnName))).thenReturn(4)
        whenever(mockCursor.getColumnIndex(eq(EXPIRY_DATE.columnName))).thenReturn(5)
        whenever(mockCursor.getColumnIndex(eq(CLASS.columnName))).thenReturn(6)

        whenever(mockCursor.getLong(eq(1))).thenReturn(cacheDateTimeStamp)
        whenever(mockCursor.getBlob(eq(2))).thenReturn(mockBlob)
        whenever(mockCursor.getInt(eq(3))).thenReturn(isCompressed)
        whenever(mockCursor.getInt(eq(4))).thenReturn(isEncrypted)
        whenever(mockCursor.getLong(eq(5))).thenReturn(expiryDate)
        whenever(mockCursor.getString(eq(6))).thenReturn(TestResponse::class.java.name)
    }

    override fun verifyGetCachedResponse(context: String,
                                         operation: Expiring,
                                         instructionToken: CacheToken,
                                         hasResponse: Boolean,
                                         isStale: Boolean,
                                         cachedResponse: ResponseWrapper<Glitch>?) {
        val queryCaptor = argumentCaptor<String>()
        verifyWithContext(mockDatabase, context).query(queryCaptor.capture())

        assertEqualsWithContext(
                "\n" +
                        "            SELECT cache_date, expiry_date, data, is_compressed, is_encrypted, class\n" +
                        "            FROM $TABLE_DEJA_VU\n" +
                        "            WHERE token = '$mockHash'\n" +
                        "            LIMIT 1\n" +
                        "            ",
                queryCaptor.firstValue.replace("\\s+", " "),
                "Query didn't match",
                context
        )
    }

}