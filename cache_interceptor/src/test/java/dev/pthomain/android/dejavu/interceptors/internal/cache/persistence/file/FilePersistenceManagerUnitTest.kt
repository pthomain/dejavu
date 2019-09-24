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

package dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.file

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.configuration.CacheInstruction
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.database.BasePersistenceManagerUnitTest
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.interceptors.internal.error.Glitch
import org.junit.Before
import java.io.File
import java.util.*

//TODO
class FilePersistenceManagerUnitTest : BasePersistenceManagerUnitTest() {

    private lateinit var mockHasher: Hasher
    private lateinit var mockCacheConfiguration: CacheConfiguration<Glitch>
    private lateinit var mockSerialisationManager: SerialisationManager<Glitch>
    private lateinit var mockDateFactory: (Long?) -> Date
    private lateinit var mockFileNameSerialiser: FileNameSerialiser
    private lateinit var mockCacheDirectory: File

    private lateinit var target: FilePersistenceManager<Glitch>

    @Before
    fun setUp() {
        mockHasher = mock()
        mockCacheConfiguration = mock()
        mockSerialisationManager = mock()
        mockDateFactory = mock()
        mockFileNameSerialiser = mock()
        mockCacheDirectory = mock()


        whenever(mockDateFactory.invoke(isNull())).thenReturn(mockCurrentDate)
        whenever(mockDateFactory.invoke(eq(mockCacheDateTime))).thenReturn(mockCacheDate)
        whenever(mockDateFactory.invoke(eq(mockExpiryDateTime))).thenReturn(mockExpiryDate)

        target = FilePersistenceManager(
                mockHasher,
                mockCacheConfiguration,
                mockSerialisationManager,
                mockDateFactory,
                mockFileNameSerialiser,
                mockCacheDirectory
        )
    }

    override fun testClearCache(useTypeToClear: Boolean,
                                clearStaleEntriesOnly: Boolean) {

    }

    override fun testCache(iteration: Int,
                           operation: CacheInstruction.Operation.Expiring,
                           encryptDataGlobally: Boolean,
                           compressDataGlobally: Boolean,
                           hasPreviousResponse: Boolean,
                           isSerialisationSuccess: Boolean) {

    }

    override fun testInvalidate(operation: CacheInstruction.Operation) {

    }

    override fun testGetCachedResponse(iteration: Int,
                                       operation: CacheInstruction.Operation.Expiring,
                                       hasResponse: Boolean,
                                       isStale: Boolean) {
    }
}