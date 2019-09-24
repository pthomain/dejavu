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
import dev.pthomain.android.dejavu.configuration.CacheInstruction
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.CacheDataHolder
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.database.BasePersistenceManagerUnitTest
import dev.pthomain.android.dejavu.interceptors.internal.error.Glitch
import java.io.File

//TODO
internal class FilePersistenceManagerUnitTest : BasePersistenceManagerUnitTest<FilePersistenceManager<Glitch>>() {

    private lateinit var mockFileNameSerialiser: FileNameSerialiser
    private lateinit var mockCacheDirectory: File

    override fun setUp(encryptDataGlobally: Boolean,
                       compressDataGlobally: Boolean,
                       cacheInstruction: CacheInstruction?): FilePersistenceManager<Glitch> {
        mockFileNameSerialiser = mock()
        mockCacheDirectory = mock()

        val mockCacheConfiguration = setUpConfiguration(
                encryptDataGlobally,
                compressDataGlobally,
                cacheInstruction
        )

        return FilePersistenceManager(
                mockHasher,
                mockCacheConfiguration,
                mockSerialisationManager,
                mockDateFactory,
                mockFileNameSerialiser,
                mockCacheDirectory
        )
    }

    override fun prepareClearCache(context: String,
                                   useTypeToClear: Boolean,
                                   clearStaleEntriesOnly: Boolean,
                                   mockClassHash: String) {
        val file1 = "file1"
        val fileList = arrayOf(file1)
        val mockCacheDataHolder = mock<CacheDataHolder>()

        whenever(mockCacheDirectory.list()).thenReturn(fileList)
        whenever(mockFileNameSerialiser.deserialise(isNull(), eq(file1))).thenReturn(mockCacheDataHolder)

    }

    override fun verifyClearCache(context: String,
                                  useTypeToClear: Boolean,
                                  clearStaleEntriesOnly: Boolean,
                                  mockClassHash: String) {


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