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

import com.nhaarman.mockitokotlin2.*
import dev.pthomain.android.dejavu.configuration.CacheInstruction
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Type.INVALIDATE
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Type.REFRESH
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.BasePersistenceManagerUnitTest
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.CacheDataHolder
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.file.FileNameSerialiser.Companion.SEPARATOR
import dev.pthomain.android.dejavu.interceptors.internal.error.Glitch
import dev.pthomain.android.dejavu.response.ResponseWrapper
import dev.pthomain.android.dejavu.retrofit.RetrofitCallAdapterFactory.Companion.INVALID_HASH
import dev.pthomain.android.dejavu.test.assertByteArrayEqualsWithContext
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import dev.pthomain.android.dejavu.test.verifyNeverWithContext
import dev.pthomain.android.dejavu.test.verifyWithContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

internal class FilePersistenceManagerUnitTest : BasePersistenceManagerUnitTest<FilePersistenceManager<Glitch>>() {

    private lateinit var mockFileNameSerialiser: FileNameSerialiser
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
    private lateinit var mockIncompleteCacheDataHolder: CacheDataHolder.Incomplete
    private lateinit var mockCompleteCacheDataHolder: CacheDataHolder.Complete

    private val mockFileWithValidHash = mockHash + SEPARATOR + "abcd"
    private val mockFileName = "mockFileName"
    private val fileOfRightType = "fileOfRightType"
    private val fileOfWrongType1 = "fileOfWrongType1"
    private val fileOfWrongType2 = "fileOfWrongType2"
    private val invalidatedFileName = "invalidatedFileName"
    private val mockFileToDelete1 = "mockFileToDelete1"
    private val mockFileToDelete2 = "mockHash_FileToDelete2"

    override fun setUp(instructionToken: CacheToken,
                       encryptDataGlobally: Boolean,
                       compressDataGlobally: Boolean,
                       cacheInstruction: CacheInstruction?): FilePersistenceManager<Glitch> {
        mockFileNameSerialiser = mock()
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

        mockIncompleteCacheDataHolder = CacheDataHolder.Incomplete(
                mockCacheDateTime,
                mockExpiryDateTime,
                mockBlob,
                INVALID_HASH,
                true,
                true
        )

        mockCompleteCacheDataHolder = with(mockIncompleteCacheDataHolder) {
            CacheDataHolder.Complete(
                    instructionToken.requestMetadata,
                    cacheDate,
                    expiryDate,
                    data,
                    responseClassHash,
                    isCompressed,
                    isEncrypted
            )
        }

        val mockCacheConfiguration = setUpConfiguration(
                encryptDataGlobally,
                compressDataGlobally,
                cacheInstruction
        )

        return FilePersistenceManager.Factory(
                mockHasher,
                mockCacheConfiguration,
                mockSerialisationManager,
                mockDateFactory,
                mockFileNameSerialiser
        ).create(mockCacheDirectory) as FilePersistenceManager<Glitch>
    }

    override fun prepareClearCache(context: String,
                                   useTypeToClear: Boolean,
                                   clearStaleEntriesOnly: Boolean,
                                   mockClassHash: String) {
        val fileList = arrayOf(fileOfWrongType1, fileOfRightType, fileOfWrongType2)
        val mockWrongCacheDataHolder1 = mock<CacheDataHolder.Incomplete>()
        val mockWrongCacheDataHolder2 = mock<CacheDataHolder.Incomplete>()
        val mockRightCacheDataHolder = mock<CacheDataHolder.Incomplete>()

        whenever(mockCacheDirectory.list()).thenReturn(fileList)
        whenever(mockFileNameSerialiser.deserialise(eq(fileOfWrongType1))).thenReturn(mockWrongCacheDataHolder1)
        whenever(mockFileNameSerialiser.deserialise(eq(fileOfWrongType2))).thenReturn(mockWrongCacheDataHolder2)
        whenever(mockFileNameSerialiser.deserialise(eq(fileOfRightType))).thenReturn(mockRightCacheDataHolder)

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

    override fun prepareCache(iteration: Int,
                              operation: Expiring,
                              encryptDataGlobally: Boolean,
                              compressDataGlobally: Boolean,
                              hasPreviousResponse: Boolean,
                              isSerialisationSuccess: Boolean) {
        if (isSerialisationSuccess) {
            whenever(mockFileNameSerialiser.serialise(any()))
                    .thenReturn(mockFileName)

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
            verifyWithContext(mockFileToDelete, context).delete()

            val dataHolderCaptor = argumentCaptor<CacheDataHolder.Complete>()
            verifyWithContext(mockFileNameSerialiser, context).serialise(dataHolderCaptor.capture())
            val cacheDataHolder = dataHolderCaptor.firstValue

            assertCacheDataHolder(
                    context,
                    instructionToken,
                    cacheDataHolder
            )

            verifyWithContext(mockOutputStream, context).write(eq(cacheDataHolder.data))
            verifyWithContext(mockOutputStream, context).flush()
        } else {
            verifyNeverWithContext(mockFileNameSerialiser, context).serialise(any())
            verifyNeverWithContext(mockFileFactory, context).invoke(any(), any())
            verifyNeverWithContext(mockFileOutputStreamFactory, context).invoke(any())
        }
    }

    private fun assertCacheDataHolder(context: String,
                                      instructionToken: CacheToken,
                                      dataHolder: CacheDataHolder.Complete) {
        assertEqualsWithContext(
                instructionToken.requestMetadata,
                dataHolder.requestMetadata,
                "RequestMetadata didn't match",
                context
        )
        assertEqualsWithContext(
                currentDateTime,
                dataHolder.cacheDate,
                "Cache date didn't match",
                context
        )
        assertEqualsWithContext(
                currentDateTime + durationInMillis,
                dataHolder.expiryDate,
                "Expiry date didn't match",
                context
        )

        assertByteArrayEqualsWithContext(
                mockBlob,
                dataHolder.data,
                context
        )
    }

    override fun prepareInvalidate(context: String,
                                   operation: Operation,
                                   instructionToken: CacheToken) {
        val fileList = arrayOf(mockFileWithValidHash)
        whenever(mockCacheDirectory.list()).thenReturn(fileList)

        whenever(mockFileNameSerialiser.deserialise(
                eq(instructionToken.requestMetadata),
                eq(mockFileWithValidHash)
        )).thenReturn(mockCompleteCacheDataHolder)

        val invalidatedHolder = mockCompleteCacheDataHolder.copy(expiryDate = 0L)

        whenever(mockFileNameSerialiser.serialise(eq(invalidatedHolder)))
                .thenReturn(invalidatedFileName)

        whenever(mockFileFactory.invoke(
                eq(mockCacheDirectory),
                eq(mockFileWithValidHash)
        )).thenReturn(mockValidFile)

        whenever(mockFileFactory.invoke(
                eq(mockCacheDirectory),
                eq(invalidatedFileName)
        )).thenReturn(mockInvalidatedFile)
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
                                          operation: Expiring,
                                          instructionToken: CacheToken,
                                          hasResponse: Boolean,
                                          isStale: Boolean,
                                          isCompressed: Int,
                                          isEncrypted: Int,
                                          cacheDateTimeStamp: Long,
                                          expiryDate: Long) {
        val fileList = arrayOf(mockFileWithValidHash)
        whenever(mockCacheDirectory.list()).thenReturn(fileList)

        whenever(mockFileFactory.invoke(eq(mockCacheDirectory), eq(mockFileWithValidHash)))
                .thenReturn(mockValidFile)

        whenever(mockFileInputStreamFactory.invoke(eq(mockValidFile)))
                .thenReturn(mockInputStream)

        whenever(mockFileReader.invoke(eq(mockInputStream))).thenReturn(mockBlob)

        whenever(mockFileNameSerialiser.deserialise(
                eq(instructionToken.requestMetadata),
                eq(mockFileWithValidHash)
        )).thenReturn(mockCompleteCacheDataHolder.copy(
                cacheDate = cacheDateTimeStamp,
                expiryDate = expiryDate,
                isCompressed = isCompressed == 1,
                isEncrypted = isEncrypted == 1
        ))
    }

    override fun verifyGetCachedResponse(context: String,
                                         operation: Expiring,
                                         instructionToken: CacheToken,
                                         hasResponse: Boolean,
                                         isStale: Boolean,
                                         cachedResponse: ResponseWrapper<Glitch>?) {
        assertEqualsWithContext(
                mockBlob,
                mockIncompleteCacheDataHolder.data,
                context
        )
    }
}