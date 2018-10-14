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

import com.google.gson.Gson
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.*
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database.DatabaseManager
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.CACHED
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheStatus.STALE
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import java.util.*

class CacheManagerUnitTest {

    private var mockDate: Date? = null
    private var mockToken: CacheToken? = null
    private var mockDateFactory: Function? = null

    private var target: CacheManager<*>? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        mockDate = mock(Date::class.java)
        mockToken = mock(CacheToken::class.java)

        mockDateFactory = mock(Function::class.java)
        target = CacheManager(
                mock<T>(DatabaseManager<*>::class.java),
                mockDateFactory,
                Gson(),
                mock(Logger::class.java)
        )
    }

    @Test
    @Throws(Exception::class)
    fun testGetCachedStatus() {
        `when`<Date>(mockToken!!.expiryDate).thenReturn(null)
        `when`<CacheStatus>(mockToken!!.status).thenReturn(CACHED)

        assertEquals(STALE, target!!.getCachedStatus(mockToken))

        resetMocks()

        `when`(mockDate!!.time).thenReturn(0L)
        `when`<Date>(mockToken!!.expiryDate).thenReturn(Date(1L))
        assertEquals(CACHED, target!!.getCachedStatus(mockToken))

        resetMocks()

        `when`(mockDate!!.time).thenReturn(1L)
        `when`<Date>(mockToken!!.expiryDate).thenReturn(Date(0L))
        assertEquals(STALE, target!!.getCachedStatus(mockToken))
    }

    private fun resetMocks() {
        reset<Any>(mockToken, mockDate, mockDateFactory)
        `when`(mockDateFactory!!.get(isNull<T>())).thenReturn(mockDate)
    }
}