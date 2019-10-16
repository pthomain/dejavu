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

package dev.pthomain.android.dejavu.interceptors.cache.persistence.memory

import android.util.LruCache
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.CacheDataHolder
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch

//TODO
internal class MemoryStoreUnitTest {

    private lateinit var mockLruCache : LruCache<String, CacheDataHolder.Incomplete>

    override fun setUpSerialisationPersistenceManager(): MemoryPersistenceManager<Glitch> {
        mockLruCache = mock()

        return MemoryPersistenceManager(
                mockHasher,
                mockDejaVuConfiguration,
                mockDateFactory,
                mockSerialisationManagerFactory,
                mockFileNameSerialiser,
                mockLruCache,
                true //TODO test flag on factory
        )
    }

    override fun prepareClearCache(entryNames: Array<String>) {
        whenever(mockCacheDirectory.list()).thenReturn(entryNames)

        whenever(mockFileFactory.invoke(eq(mockCacheDirectory), eq(fileOfRightType))).thenReturn(mockFileOfRightType)
        whenever(mockFileFactory.invoke(eq(mockCacheDirectory), eq(fileOfWrongType1))).thenReturn(mockFileOfWrongType1)
        whenever(mockFileFactory.invoke(eq(mockCacheDirectory), eq(fileOfWrongType2))).thenReturn(mockFileOfWrongType2)
    }

    override fun prepareCache() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun verifyCache(context: String, cacheDataHolder: CacheDataHolder.Complete?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun prepareInvalidate(context: String, fileList: Array<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun prepareGetCachedResponse(context: String, fileList: Array<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun verifyClearCache(context: String, useTypeToClear: Boolean, clearStaleEntriesOnly: Boolean, mockClassHash: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun verifyCheckInvalidation(context: String, operation: Operation, instructionToken: CacheToken) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}