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

import com.nhaarman.mockitokotlin2.*
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Expiring
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.persistence.BasePersistenceManagerUnitTest
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.KeyValuePersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.file.FileNameSerialiser.Companion.SEPARATOR
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager.Factory.Type.FILE
import dev.pthomain.android.dejavu.interceptors.error.Glitch
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.CacheDataHolder
import dev.pthomain.android.dejavu.retrofit.RetrofitCallAdapterFactory.Companion.INVALID_HASH
import dev.pthomain.android.dejavu.test.assertByteArrayEqualsWithContext
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import dev.pthomain.android.dejavu.test.verifyNeverWithContext
import dev.pthomain.android.dejavu.test.verifyWithContext

internal abstract class KeyValuePersistenceManagerUnitTest<T : KeyValuePersistenceManager<Glitch>>
    : BasePersistenceManagerUnitTest<T>() {

    private lateinit var mockIncompleteCacheDataHolder: CacheDataHolder.Incomplete
    private lateinit var mockCompleteCacheDataHolder: CacheDataHolder.Complete

    protected lateinit var mockFileNameSerialiser: FileNameSerialiser
    protected lateinit var mockDejaVuConfiguration: DejaVuConfiguration<Glitch>

    protected val mockFileWithValidHash = mockHash + SEPARATOR + "abcd"
    protected val mockFileName = "mockFileName"
    protected val fileOfRightType = "fileOfRightType"
    protected val fileOfWrongType1 = "fileOfWrongType1"
    protected val fileOfWrongType2 = "fileOfWrongType2"
    protected val invalidatedFileName = "invalidatedFileName"
    protected val mockFileToDelete1 = "mockFileToDelete1"
    protected val mockFileToDelete2 = "mockHash_FileToDelete2"

    final override fun setUp(instructionToken: CacheToken,
                             encryptDataGlobally: Boolean,
                             compressDataGlobally: Boolean,
                             cacheInstruction: CacheInstruction?): T {
        mockFileNameSerialiser = mock()

        mockIncompleteCacheDataHolder = CacheDataHolder.Incomplete(
                mockCacheDateTime,
                mockExpiryDateTime,
                mockBlob,
                INVALID_HASH,
                true,
                true
        )

        whenever(mockSerialisationManagerFactory.create(eq(FILE))).thenReturn(mockSerialisationManager)

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

        mockDejaVuConfiguration = setUpConfiguration(
                encryptDataGlobally,
                compressDataGlobally,
                cacheInstruction
        )

        return setUpSerialisationPersistenceManager()
    }

    protected abstract fun setUpSerialisationPersistenceManager(): T

    final override fun prepareClearCache(context: String,
                                         useTypeToClear: Boolean,
                                         clearStaleEntriesOnly: Boolean,
                                         mockClassHash: String) {
        val fileList = arrayOf(fileOfWrongType1, fileOfRightType, fileOfWrongType2)
        val mockWrongCacheDataHolder1 = mock<CacheDataHolder.Incomplete>()
        val mockWrongCacheDataHolder2 = mock<CacheDataHolder.Incomplete>()
        val mockRightCacheDataHolder = mock<CacheDataHolder.Incomplete>()

        prepareClearCache(fileList)

        whenever(mockFileNameSerialiser.deserialise(eq(fileOfWrongType1))).thenReturn(mockWrongCacheDataHolder1)
        whenever(mockFileNameSerialiser.deserialise(eq(fileOfWrongType2))).thenReturn(mockWrongCacheDataHolder2)
        whenever(mockFileNameSerialiser.deserialise(eq(fileOfRightType))).thenReturn(mockRightCacheDataHolder)

        if (useTypeToClear) {
            whenever(mockRightCacheDataHolder.responseClassHash).thenReturn(mockClassHash)
            whenever(mockWrongCacheDataHolder1.responseClassHash).thenReturn("wrong1")
            whenever(mockWrongCacheDataHolder2.responseClassHash).thenReturn("wrong2")
        }

    }

    protected abstract fun prepareClearCache(entryNames: Array<String>)

    final override fun prepareCache(iteration: Int,
                                    operation: Expiring,
                                    encryptDataGlobally: Boolean,
                                    compressDataGlobally: Boolean,
                                    hasPreviousResponse: Boolean,
                                    isSerialisationSuccess: Boolean) {
        if (isSerialisationSuccess) {
            whenever(mockFileNameSerialiser.serialise(any()))
                    .thenReturn(mockFileName)

            prepareCache()
        }
    }

    protected abstract fun prepareCache()

    final override fun verifyCache(context: String,
                                   iteration: Int,
                                   instructionToken: CacheToken,
                                   operation: Expiring,
                                   encryptData: Boolean,
                                   compressData: Boolean,
                                   hasPreviousResponse: Boolean,
                                   isSerialisationSuccess: Boolean,
                                   duration: Long) {
        if (isSerialisationSuccess) {
            val dataHolderCaptor = argumentCaptor<CacheDataHolder.Complete>()
            verifyWithContext(mockFileNameSerialiser, context).serialise(dataHolderCaptor.capture())
            val cacheDataHolder = dataHolderCaptor.firstValue

            assertCacheDataHolder(
                    context,
                    instructionToken,
                    cacheDataHolder
            )

            verifyCache(context, cacheDataHolder)
        } else {
            verifyNeverWithContext(mockFileNameSerialiser, context).serialise(any())
            verifyCache(context, null)
        }
    }

    abstract fun verifyCache(context: String,
                             cacheDataHolder: CacheDataHolder.Complete?)


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

    final override fun prepareInvalidate(context: String,
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

        prepareInvalidate(context, fileList)
    }

    abstract fun prepareInvalidate(context: String,
                                   fileList: Array<String>)

    final override fun prepareCheckInvalidation(context: String,
                                                operation: Operation,
                                                instructionToken: CacheToken) {
        prepareInvalidate(
                context,
                operation,
                instructionToken
        )
    }

    final override fun prepareGetCachedResponse(context: String,
                                                operation: Expiring,
                                                instructionToken: CacheToken,
                                                hasResponse: Boolean,
                                                isStale: Boolean,
                                                isCompressed: Int,
                                                isEncrypted: Int,
                                                cacheDateTimeStamp: Long,
                                                expiryDate: Long) {
        val fileList = arrayOf(mockFileWithValidHash)

        prepareGetCachedResponse(context, fileList)

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

    abstract fun prepareGetCachedResponse(context: String,
                                          fileList: Array<String>)

    final override fun verifyGetCachedResponse(context: String,
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