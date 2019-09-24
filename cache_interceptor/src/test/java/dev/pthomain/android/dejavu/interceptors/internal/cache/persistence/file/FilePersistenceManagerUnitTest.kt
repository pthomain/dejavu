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
import dev.pthomain.android.dejavu.test.verifyNeverWithContext
import dev.pthomain.android.dejavu.test.verifyWithContext
import java.io.File

//TODO
internal class FilePersistenceManagerUnitTest : BasePersistenceManagerUnitTest<FilePersistenceManager<Glitch>>() {

    private lateinit var mockFileNameSerialiser: FileNameSerialiser
    private lateinit var mockCacheDirectory: File
    private lateinit var mockFileFactory: (File, String) -> File
    private lateinit var mockFileOfRightType: File
    private lateinit var mockFileOfWrongType1: File
    private lateinit var mockFileOfWrongType2: File

    private val fileOfRightType = "fileOfRightType"
    private val fileOfWrongType1 = "fileOfWrongType1"
    private val fileOfWrongType2 = "fileOfWrongType2"

    override fun setUp(encryptDataGlobally: Boolean,
                       compressDataGlobally: Boolean,
                       cacheInstruction: CacheInstruction?): FilePersistenceManager<Glitch> {
        mockFileNameSerialiser = mock()
        mockCacheDirectory = mock()
        mockFileFactory = mock()
        mockFileOfRightType = mock()
        mockFileOfWrongType1 = mock()
        mockFileOfWrongType2 = mock()

        val mockCacheConfiguration = setUpConfiguration(
                encryptDataGlobally,
                compressDataGlobally,
                cacheInstruction
        )

        return FilePersistenceManager(
                mockHasher,
                mockFileFactory,
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
        val fileList = arrayOf(fileOfWrongType1, fileOfRightType, fileOfWrongType2)
        val mockWrongCacheDataHolder1 = mock<CacheDataHolder>()
        val mockWrongCacheDataHolder2 = mock<CacheDataHolder>()
        val mockRightCacheDataHolder = mock<CacheDataHolder>()

        whenever(mockCacheDirectory.list()).thenReturn(fileList)
        whenever(mockFileNameSerialiser.deserialise(isNull(), eq(fileOfWrongType1))).thenReturn(mockWrongCacheDataHolder1)
        whenever(mockFileNameSerialiser.deserialise(isNull(), eq(fileOfWrongType2))).thenReturn(mockWrongCacheDataHolder2)
        whenever(mockFileNameSerialiser.deserialise(isNull(), eq(fileOfRightType))).thenReturn(mockRightCacheDataHolder)

        if (useTypeToClear) {
            whenever(mockRightCacheDataHolder.responseClassHash).thenReturn(mockClassHash)
            whenever(mockWrongCacheDataHolder1.responseClassHash).thenReturn("wrong1")
            whenever(mockWrongCacheDataHolder2.responseClassHash).thenReturn("wrong2")
        }

        whenever(mockFileFactory.invoke(eq(mockCacheDirectory), eq(fileOfRightType))).thenReturn(mockFileOfRightType)
        whenever(mockFileFactory.invoke(eq(mockCacheDirectory), eq(fileOfWrongType1))).thenReturn(mockFileOfWrongType1)
        whenever(mockFileFactory.invoke(eq(mockCacheDirectory), eq(fileOfWrongType2))).thenReturn(mockFileOfWrongType2)
    }

    override fun verifyClearCache(context: String,
                                  useTypeToClear: Boolean,
                                  clearStaleEntriesOnly: Boolean,
                                  mockClassHash: String) {
        verifyWithContext(mockFileOfRightType, context).delete()

        if (!useTypeToClear) {
            verifyWithContext(mockFileOfWrongType1, context).delete()
            verifyWithContext(mockFileOfWrongType2, context).delete()
        } else {
            verifyNeverWithContext(mockFileFactory, context).invoke(eq(mockCacheDirectory), eq(fileOfWrongType1))
            verifyNeverWithContext(mockFileFactory, context).invoke(eq(mockCacheDirectory), eq(fileOfWrongType2))
        }
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