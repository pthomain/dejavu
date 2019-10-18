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

package dev.pthomain.android.dejavu.interceptors.cache.persistence.base

import com.nhaarman.mockitokotlin2.*
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Expiring
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Type.INVALIDATE
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Type.REFRESH
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.persistence.BasePersistenceManagerUnitTest
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.CacheDataHolder.Complete
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.CacheDataHolder.Incomplete
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.FileNameSerialiser
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.FileNameSerialiser.Companion.SEPARATOR
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager.Factory.Type.FILE
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.retrofit.RetrofitCallAdapterFactory.Companion.INVALID_HASH
import dev.pthomain.android.dejavu.test.assertByteArrayEqualsWithContext
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import dev.pthomain.android.dejavu.test.verifyNeverWithContext
import dev.pthomain.android.dejavu.test.verifyWithContext

internal class KeyValuePersistenceManagerUnitTest
    : BasePersistenceManagerUnitTest<KeyValuePersistenceManager<Glitch>>() {

    private lateinit var mockIncompleteCacheDataHolder: Incomplete
    private lateinit var mockCompleteCacheDataHolder: Complete
    private lateinit var mockFileNameSerialiser: FileNameSerialiser
    private lateinit var mockDejaVuConfiguration: DejaVuConfiguration<Glitch>
    private lateinit var mockKeyValueStore: KeyValueStore<String, String, Incomplete>

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
                       cacheInstruction: CacheInstruction<*>?): KeyValuePersistenceManager<Glitch> {
        mockFileNameSerialiser = mock()

        mockIncompleteCacheDataHolder = Incomplete(
                mockCacheDateTime,
                mockExpiryDateTime,
                mockBlob,
                INVALID_HASH,
                true,
                true
        )

        whenever(mockSerialisationManagerFactory.create(eq(FILE))).thenReturn(mockSerialisationManager)

        mockCompleteCacheDataHolder = with(mockIncompleteCacheDataHolder) {
            Complete(
                    instructionToken.requestMetadata,
                    cacheDate,
                    expiryDate,
                    data,
                    responseClassHash,
                    isCompressed,
                    isEncrypted
            )
        }

        mockDejaVuConfiguration = setUpConfiguration(
                encryptDataGlobally,
                compressDataGlobally,
                cacheInstruction
        )

        mockKeyValueStore = mock()

        return KeyValuePersistenceManager(
                mockHasher,
                mockDejaVuConfiguration,
                mockDateFactory,
                mockFileNameSerialiser,
                mockKeyValueStore,
                mockSerialisationManager
        )
    }

    override fun prepareClearCache(context: String,
                                   useTypeToClear: Boolean,
                                   clearStaleEntriesOnly: Boolean,
                                   mockClassHash: String) {
        val fileList = arrayOf(fileOfWrongType1, fileOfRightType, fileOfWrongType2)
        val mockWrongCacheDataHolder1 = mock<Incomplete>()
        val mockWrongCacheDataHolder2 = mock<Incomplete>()
        val mockRightCacheDataHolder = mock<Incomplete>()

        whenever(mockKeyValueStore.values()).thenReturn(mapOf(
                fileList[0] to mockWrongCacheDataHolder1,
                fileList[1] to mockRightCacheDataHolder,
                fileList[2] to mockWrongCacheDataHolder2
        ))

        whenever(mockFileNameSerialiser.deserialise(eq(fileOfWrongType1))).thenReturn(mockWrongCacheDataHolder1)
        whenever(mockFileNameSerialiser.deserialise(eq(fileOfWrongType2))).thenReturn(mockWrongCacheDataHolder2)
        whenever(mockFileNameSerialiser.deserialise(eq(fileOfRightType))).thenReturn(mockRightCacheDataHolder)

        if (useTypeToClear) {
            whenever(mockRightCacheDataHolder.responseClassHash).thenReturn(mockClassHash)
            whenever(mockWrongCacheDataHolder1.responseClassHash).thenReturn("wrong1")
            whenever(mockWrongCacheDataHolder2.responseClassHash).thenReturn("wrong2")
        }
    }

    override fun verifyClearCache(context: String,
                                  useTypeToClear: Boolean,
                                  clearStaleEntriesOnly: Boolean,
                                  mockClassHash: String) {
        verifyWithContext(mockKeyValueStore, context).delete(eq(fileOfRightType))

        if (!useTypeToClear) {
            verifyWithContext(mockKeyValueStore, context).delete(eq(fileOfWrongType1))
            verifyWithContext(mockKeyValueStore, context).delete(eq(fileOfWrongType2))
        } else {
            verifyNeverWithContext(mockKeyValueStore, context).delete(eq(fileOfWrongType1))
            verifyNeverWithContext(mockKeyValueStore, context).delete(eq(fileOfWrongType2))
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
            val dataHolderCaptor = argumentCaptor<Complete>()
            verifyWithContext(mockFileNameSerialiser, context).serialise(dataHolderCaptor.capture())
            val cacheDataHolder = dataHolderCaptor.firstValue

            assertCacheDataHolder(
                    context,
                    instructionToken,
                    cacheDataHolder
            )

            verifyWithContext(mockFileToDelete, context).delete()
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
                                      dataHolder: Complete) {
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

        whenever(mockFileNameSerialiser.deserialise(
                eq(instructionToken.requestMetadata),
                eq(mockFileWithValidHash)
        )).thenReturn(mockCompleteCacheDataHolder)

        val invalidatedHolder = mockCompleteCacheDataHolder.copy(expiryDate = 0L)

        whenever(mockFileNameSerialiser.serialise(eq(invalidatedHolder)))
                .thenReturn(invalidatedFileName)

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
