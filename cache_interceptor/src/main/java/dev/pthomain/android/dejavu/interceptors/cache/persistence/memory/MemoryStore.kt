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
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.CacheDataHolder
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.KeyValueStore
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.FileNameSerialiser.Companion.SEPARATOR

class MemoryStore internal constructor(
        private val lruCache: LruCache<String, CacheDataHolder.Incomplete>
) : KeyValueStore<String, String, CacheDataHolder.Incomplete> {

    /**
     * Returns an existing entry key matching the given partial key
     *
     * @param partialKey the partial key used to retrieve the full key
     * @return the matching full key if present
     */
    override fun findPartialKey(partialKey: String) =
            lruCache.snapshot()
                    .entries
                    .firstOrNull { it.key.startsWith(partialKey + SEPARATOR) }
                    ?.key

    /**
     * Returns an entry for the given key, if present
     *
     * @param key the entry's key
     * @return the matching entry if present
     */
    override fun get(key: String) =
            lruCache.get(key)

    /**
     * Saves an entry with a given key
     *
     * @param key the key to save the entry under
     * @param value the value to associate with the key
     */
    override fun save(key: String, value: CacheDataHolder.Incomplete) {
        lruCache.put(key, value)
    }

    /**
     * @return a map of the existing entries
     */
    override fun values() =
            lruCache.snapshot()

    /**
     * Deletes the entry matching the given key
     *
     * @param key the entry's key
     */
    override fun delete(key: String) {
        lruCache.remove(key)
    }

    /**
     * Renames an entry
     *
     * @param oldKey the old entry key
     * @param newKey  the new entry key
     */
    @Synchronized
    override fun rename(oldKey: String,
                        newKey: String) {
        val oldEntry = lruCache.get(oldKey)
        delete(oldKey)
        lruCache.put(newKey, oldEntry)
    }

    class Factory {
        fun create(maxEntries: Int = 20) = MemoryStore(
                LruCache(maxEntries)
        )
    }
}