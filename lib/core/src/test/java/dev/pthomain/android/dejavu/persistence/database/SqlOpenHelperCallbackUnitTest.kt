/*
 *
 *  Copyright (C) 2017-2020 Pierre Thomain
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

package dev.pthomain.android.dejavu.persistence.database

import androidx.sqlite.db.SupportSQLiteDatabase
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Test
import org.mockito.InOrder

class SqlOpenHelperCallbackUnitTest {

    private lateinit var mockSupportSQLiteDatabase: SupportSQLiteDatabase

    private lateinit var target: dev.pthomain.android.dejavu.persistence.sqlite.SqlOpenHelperCallback

    @Before
    fun setUp() {
        mockSupportSQLiteDatabase = mock()

        target = dev.pthomain.android.dejavu.persistence.sqlite.SqlOpenHelperCallback(1234)
    }

    @Test
    fun testOnCreate() {
        target.onCreate(mockSupportSQLiteDatabase)

        verifyOnCreate()
    }

    private fun verifyOnCreate(inOrder: InOrder = inOrder(mockSupportSQLiteDatabase)) {
        inOrder.apply {
            verify(mockSupportSQLiteDatabase).execSQL(
                    eq("CREATE TABLE IF NOT EXISTS dejavu (token TEXT UNIQUE, cache_date INTEGER, expiry_date INTEGER, data NONE, class TEXT, is_encrypted INTEGER, is_compressed INTEGER)")
            )
            verify(mockSupportSQLiteDatabase).execSQL(
                    eq("CREATE INDEX IF NOT EXISTS token_index ON dejavu(token)")
            )
            verify(mockSupportSQLiteDatabase).execSQL(
                    eq("CREATE INDEX IF NOT EXISTS expiry_date_index ON dejavu(expiry_date)")
            )
        }
    }

    @Test
    fun testOnUpgrade() {
        target.onUpgrade(
                mockSupportSQLiteDatabase,
                1234,
                1235
        )

        verifyOnUpgradeDowngrade()
    }

    private fun verifyOnUpgradeDowngrade() {
        val inOrder = inOrder(mockSupportSQLiteDatabase)
        inOrder.verify(mockSupportSQLiteDatabase).execSQL("DROP TABLE IF EXISTS dejavu")

        verifyOnCreate(inOrder)
    }

    @Test
    fun testOnDowngrade() {
        target.onDowngrade(
                mockSupportSQLiteDatabase,
                1234,
                1235
        )

        verifyOnUpgradeDowngrade()
    }

}