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

package dev.pthomain.android.dejavu.persistence.base

import com.nhaarman.mockitokotlin2.*
import dev.pthomain.android.dejavu.configuration.error.glitch.Glitch
import dev.pthomain.android.dejavu.persistence.BasePersistenceManagerUnitTest
import dev.pthomain.android.dejavu.persistence.base.CacheDataHolder.Complete
import dev.pthomain.android.dejavu.persistence.base.CacheDataHolder.Incomplete
import dev.pthomain.android.dejavu.persistence.base.store.KeyValuePersistenceManager
import dev.pthomain.android.dejavu.persistence.base.store.KeyValueStore
import dev.pthomain.android.dejavu.serialisation.KeySerialiser
import dev.pthomain.android.dejavu.serialisation.KeySerialiser.Companion.SEPARATOR
import dev.pthomain.android.dejavu.shared.token.InstructionToken
import dev.pthomain.android.dejavu.shared.token.instruction.INVALID_HASH
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.test.assertByteArrayEqualsWithContext
import dev.pthomain.android.dejavu.test.assertEqualsWithContext
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.dejavu.test.verifyNeverWithContext
import dev.pthomain.android.dejavu.test.verifyWithContext

//FIXME
internal class KeyValuePersistenceManagerUnitTest
    : BasePersistenceManagerUnitTest<KeyValuePersistenceManager<Glitch>>() {

    private lateinit var mockRightCacheDataHolder: Incomplete
    private lateinit var mockWrongCacheDataHolder2: Incomplete
    private lateinit var mockWrongCacheDataHolder1: Incomplete
    private lateinit var mockIncompleteCacheDataHolder: Incomplete
    private lateinit var mockCompleteCacheDataHolder: Complete
    private lateinit var mockKeySerialiser: dev.pthomain.android.dejavu.serialisation.KeySerialiser
    private lateinit var mockDejaVu.Configuration: DejaVu.Configuration<Glitch>
    private lateinit var mockKeyValueStore: KeyValueStore<String, String, Incomplete>

    private val mockEntryWithValidHash = mockHash + SEPARATOR + "abcd"
    private val unrelatedEntryName = "unrelatedEntryName"
    private val entryOfRightType = "EntryOfRightType"
    private val entryOfWrongType1 = "EntryOfWrongType1"
    private val entryOfWrongType2 = "EntryOfWrongType2"
    private val invalidatedEntryName = "invalidatedEntryName"

    override fun setUp(instructionToken: InstructionToken): KeyValuePersistenceManager<Glitch> {
        mockKeySerialiser = mock()

        mockIncompleteCacheDataHolder = Incomplete(
                mockCacheDateTime,
                mockExpiryDateTime,
                mockBlob,
                INVALID_HASH,
                INVALID_HASH,
                true,
                true
        )

        mockDejaVu.Configuration = setUpConfiguration(instructionToken.instruction)

        mockCompleteCacheDataHolder = with(mockIncompleteCacheDataHolder) {
            Complete(
                    instructionToken.instruction.requestMetadata,
                    cacheDate,
                    expiryDate,
                    data,
                    isCompressed,
                    isEncrypted
            )
        }

        mockKeyValueStore = mock()

        mockWrongCacheDataHolder1 = mock()
        mockWrongCacheDataHolder2 = mock()
        mockRightCacheDataHolder = mock()

        return KeyValuePersistenceManager(
                mockDejaVu.Configuration,
                mockDateFactory,
                mockKeySerialiser,
                mockKeyValueStore,
                mockSerialisationManager
        )
    }

    override fun prepareClearCache(context: String,
                                   instructionToken: InstructionToken) {
        val entryList = arrayOf(entryOfWrongType1, entryOfRightType, entryOfWrongType2)

        whenever(mockKeyValueStore.values()).thenReturn(mapOf(
                entryList[0] to mockWrongCacheDataHolder1,
                entryList[1] to mockRightCacheDataHolder,
                entryList[2] to mockWrongCacheDataHolder2
        ))

        whenever(mockKeySerialiser.deserialise(eq(entryOfWrongType1))).thenReturn(mockWrongCacheDataHolder1)
        whenever(mockKeySerialiser.deserialise(eq(entryOfWrongType2))).thenReturn(mockWrongCacheDataHolder2)
        whenever(mockKeySerialiser.deserialise(eq(entryOfRightType))).thenReturn(mockRightCacheDataHolder)

        val requestMetadata = instructionToken.instruction.requestMetadata
        if (requestMetadata.responseClass == TestResponse::class.java) {
            whenever(mockRightCacheDataHolder.responseClassHash).thenReturn(requestMetadata.classHash)
            whenever(mockWrongCacheDataHolder1.responseClassHash).thenReturn("wrong1")
            whenever(mockWrongCacheDataHolder2.responseClassHash).thenReturn("wrong2")
        }
    }

    override fun verifyClearCache(context: String,
                                  useTypeToClear: Boolean,
                                  clearStaleEntriesOnly: Boolean,
                                  mockClassHash: String) {
        verifyWithContext(mockKeyValueStore, context).delete(eq(entryOfRightType))

        if (!useTypeToClear) {
            verifyWithContext(mockKeyValueStore, context).delete(eq(entryOfWrongType1))
            verifyWithContext(mockKeyValueStore, context).delete(eq(entryOfWrongType2))
        } else {
            verifyNeverWithContext(mockKeyValueStore, context).delete(eq(entryOfWrongType1))
            verifyNeverWithContext(mockKeyValueStore, context).delete(eq(entryOfWrongType2))
        }
    }

    override fun prepareCache(iteration: Int,
                              operation: Cache,
                              hasPreviousResponse: Boolean,
                              isSerialisationSuccess: Boolean) {
        if (isSerialisationSuccess) {
            whenever(mockKeySerialiser.serialise(any()))
                    .thenReturn(mockEntryWithValidHash)

            whenever(mockKeyValueStore.get(eq(mockEntryWithValidHash))).thenReturn(mockRightCacheDataHolder)

            whenever(mockKeyValueStore.values()).thenReturn(mapOf(
                    unrelatedEntryName to mockWrongCacheDataHolder1,
                    mockEntryWithValidHash to mockRightCacheDataHolder
            ))
        }
    }

    override fun verifyCache(context: String,
                             iteration: Int,
                             instructionToken: InstructionToken,
                             operation: Cache,
                             encryptData: Boolean,
                             compressData: Boolean,
                             hasPreviousResponse: Boolean,
                             isSerialisationSuccess: Boolean) {
        if (isSerialisationSuccess) {
            val dataHolderCaptor = argumentCaptor<Complete>()
            verifyWithContext(mockKeySerialiser, context).serialise(dataHolderCaptor.capture())
            val cacheDataHolder = dataHolderCaptor.firstValue

            assertCacheDataHolder(
                    context,
                    instructionToken,
                    cacheDataHolder
            )

            verifyWithContext(mockKeyValueStore, context).delete(eq(mockEntryWithValidHash))

            val argumentCaptor = argumentCaptor<Incomplete>()

            verifyWithContext(mockKeyValueStore, context)
                    .save(eq(mockEntryWithValidHash), argumentCaptor.capture())

            //TODO verify capture

        } else {
            verifyNeverWithContext(mockKeyValueStore, context).delete(eq(mockEntryWithValidHash))
        }

        verifyNeverWithContext(mockKeyValueStore, context).delete(eq(unrelatedEntryName))
    }

    private fun assertCacheDataHolder(context: String,
                                      instructionToken: InstructionToken,
                                      dataHolder: Complete) {
        assertEqualsWithContext(
                instructionToken.instruction.requestMetadata,
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

        val operation = instructionToken.instruction.operation as Cache

        assertEqualsWithContext(
                currentDateTime + operation.durationInSeconds,
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
                                   instructionToken: InstructionToken) {
        val entryList = mapOf(
                mockEntryWithValidHash to mockRightCacheDataHolder
        )

        whenever(mockKeySerialiser.deserialise(
                eq(instructionToken.instruction.requestMetadata),
                eq(mockEntryWithValidHash)
        )).thenReturn(mockCompleteCacheDataHolder)

        val invalidatedHolder = mockCompleteCacheDataHolder.copy(expiryDate = 0L)

        whenever(mockKeySerialiser.serialise(eq(invalidatedHolder)))
                .thenReturn(invalidatedEntryName)

        whenever(mockKeyValueStore.values()).thenReturn(entryList)

//        whenever(mockKeyValueStore.get(
//                eq(mockEntryWithValidHash)
//        )).thenReturn(mockValidEntry)
//
//        whenever(mockFileFactory.invoke(
//                eq(mockCacheDirectory),
//                eq(invalidatedEntryName)
//        )).thenReturn(mockInvalidatedEntry)
    }

    override fun prepareCheckInvalidation(context: String,
                                          operation: Operation,
                                          instructionToken: InstructionToken) {
        prepareInvalidate(
                context,
                operation,
                instructionToken
        )
    }

    override fun verifyCheckInvalidation(context: String,
                                         operation: Operation,
                                         instructionToken: InstructionToken) {
//        if (operation.type == INVALIDATE || operation.type == REFRESH) {
//            verifyWithContext(
//                    mockValidEntry,
//                    context
//            ).renameTo(eq(mockInvalidatedEntry))
//        } else {
//            verifyNeverWithContext(
//                    mockValidEntry,
//                    context
//            ).renameTo(any())
//        }
    }

    override fun prepareGetCachedResponse(context: String,
                                          operation: Cache,
                                          instructionToken: InstructionToken,
                                          hasResponse: Boolean,
                                          isStale: Boolean,
                                          isCompressed: Int,
                                          isEncrypted: Int,
                                          cacheDateTimeStamp: Long,
                                          expiryDate: Long) {
        val EntryList = arrayOf(mockEntryWithValidHash)

//        whenever(mockCacheDirectory.list()).thenReturn(EntryList)
//
//        whenever(mockFileFactory.invoke(eq(mockCacheDirectory), eq(mockEntryWithValidHash)))
//                .thenReturn(mockValidEntry)
//
//        whenever(mockFileInputStreamFactory.invoke(eq(mockValidEntry)))
//                .thenReturn(mockInputStream)
//
//        whenever(mockFileReader.invoke(eq(mockInputStream))).thenReturn(mockBlob)

        whenever(mockKeySerialiser.deserialise(
                eq(instructionToken.instruction.requestMetadata),
                eq(mockEntryWithValidHash)
        )).thenReturn(mockCompleteCacheDataHolder.copy(
                cacheDate = cacheDateTimeStamp,
                expiryDate = expiryDate,
                isCompressed = isCompressed == 1,
                isEncrypted = isEncrypted == 1
        ))
    }

    override fun verifyGetCachedResponse(context: String,
                                         operation: Cache,
                                         instructionToken: InstructionToken,
                                         hasResponse: Boolean,
                                         isStale: Boolean,
                                         cachedResponse: ResponseWrapper<*, *, Glitch>?) {
        assertEqualsWithContext(
                mockBlob,
                mockIncompleteCacheDataHolder.data,
                context
        )
    }

}
