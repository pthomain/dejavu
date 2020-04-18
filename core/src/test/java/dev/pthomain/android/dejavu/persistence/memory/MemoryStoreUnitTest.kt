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

package dev.pthomain.android.dejavu.persistence.memory

import android.util.LruCache
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.dejavu.persistence.base.BaseKeyValueStoreUnitTest
import dev.pthomain.android.dejavu.persistence.base.CacheDataHolder.Incomplete

internal class MemoryStoreUnitTest : BaseKeyValueStoreUnitTest<MemoryStore>() {

    private lateinit var mockLruCache: LruCache<String, Incomplete>
    private lateinit var mockLruCacheFactory: (Int) -> LruCache<String, Incomplete>

    override fun setUpTarget(): MemoryStore {
        mockLruCache = mock()
        mockLruCacheFactory = mock()

        whenever(mockLruCacheFactory.invoke(eq(20))).thenReturn(mockLruCache)

        return MemoryStore.Factory(mockLruCacheFactory).create(20)
    }

    //TODO + integration

}
