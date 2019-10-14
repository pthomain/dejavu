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

package dev.pthomain.android.dejavu.interceptors.cache.persistence.file

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Type.INVALIDATE
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Type.REFRESH
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.error.Glitch
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.CacheDataHolder
import dev.pthomain.android.dejavu.test.verifyNeverWithContext
import dev.pthomain.android.dejavu.test.verifyWithContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

internal class FilePersistenceManagerUnitTest : KeyValuePersistenceManagerUnitTest<FilePersistenceManager<Glitch>>() {

    private lateinit var mockCacheDirectory: File
    private lateinit var mockFileFactory: (File, String) -> File
    private lateinit var mockFileOfRightType: File
    private lateinit var mockFileOfWrongType1: File
    private lateinit var mockFileOfWrongType2: File
    private lateinit var mockValidFile: File
    private lateinit var mockFileToDelete: File
    private lateinit var mockInvalidatedFile: File
    private lateinit var mockFileInputStreamFactory: (File) -> InputStream
    private lateinit var mockFileOutputStreamFactory: (File) -> OutputStream
    private lateinit var mockFileReader: (InputStream) -> ByteArray
    private lateinit var mockOutputStream: OutputStream
    private lateinit var mockInputStream: InputStream

    override fun setUpSerialisationPersistenceManager(): FilePersistenceManager<Glitch> {
        mockCacheDirectory = mock()
        mockFileFactory = mock()
        mockFileOfRightType = mock()
        mockFileOfWrongType1 = mock()
        mockFileOfWrongType2 = mock()
        mockFileInputStreamFactory = mock()
        mockFileOutputStreamFactory = mock()
        mockFileReader = mock()
        mockInputStream = mock()
        mockOutputStream = mock()
        mockValidFile = mock()
        mockInvalidatedFile = mock()
        mockFileToDelete = mock()

        return FilePersistenceManager.Factory(
                mockHasher,
                mockDejaVuConfiguration,
                mockSerialisationManagerFactory,
                mockDateFactory,
                mockFileNameSerialiser
        ).create(mockCacheDirectory) as FilePersistenceManager<Glitch>
    }

    override fun prepareClearCache(entryNames: Array<String>) {
        whenever(mockCacheDirectory.list()).thenReturn(entryNames)

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

    override fun prepareCache() {
        whenever(mockFileFactory.invoke(eq(mockCacheDirectory), eq(mockFileName)))
                .thenReturn(mockFileOfRightType)

        whenever(mockFileOutputStreamFactory.invoke(eq(mockFileOfRightType)))
                .thenReturn(mockOutputStream)

        whenever(mockCacheDirectory.list()).thenReturn(arrayOf(
                mockFileToDelete1,
                mockFileToDelete2
        ))

        whenever(mockFileFactory.invoke(
                eq(mockCacheDirectory),
                eq(mockFileToDelete2)
        )).thenReturn(mockFileToDelete)
    }

    override fun verifyCache(context: String,
                             cacheDataHolder: CacheDataHolder.Complete?) {
        if (cacheDataHolder == null) {
            verifyNeverWithContext(mockFileFactory, context).invoke(any(), any())
            verifyNeverWithContext(mockFileOutputStreamFactory, context).invoke(any())
        } else {
            verifyWithContext(mockFileToDelete, context).delete()
            verifyWithContext(mockOutputStream, context).write(eq(cacheDataHolder.data))
            verifyWithContext(mockOutputStream, context).flush()
        }
    }

    override fun prepareInvalidate(context: String,
                                   fileList: Array<String>) {
        whenever(mockCacheDirectory.list()).thenReturn(fileList)

        whenever(mockFileFactory.invoke(
                eq(mockCacheDirectory),
                eq(mockFileWithValidHash)
        )).thenReturn(mockValidFile)

        whenever(mockFileFactory.invoke(
                eq(mockCacheDirectory),
                eq(invalidatedFileName)
        )).thenReturn(mockInvalidatedFile)
    }

    override fun verifyCheckInvalidation(context: String,
                                         operation: CacheInstruction.Operation,
                                         instructionToken: CacheToken) {
        if (operation.type == INVALIDATE || operation.type == REFRESH) {
            verifyWithContext(
                    mockValidFile,
                    context
            ).renameTo(eq(mockInvalidatedFile))
        } else {
            verifyNeverWithContext(
                    mockValidFile,
                    context
            ).renameTo(any())
        }
    }

    override fun prepareGetCachedResponse(context: String,
                                          fileList: Array<String>) {
        whenever(mockCacheDirectory.list()).thenReturn(fileList)

        whenever(mockFileFactory.invoke(eq(mockCacheDirectory), eq(mockFileWithValidHash)))
                .thenReturn(mockValidFile)

        whenever(mockFileInputStreamFactory.invoke(eq(mockValidFile)))
                .thenReturn(mockInputStream)

        whenever(mockFileReader.invoke(eq(mockInputStream))).thenReturn(mockBlob)
    }
}


