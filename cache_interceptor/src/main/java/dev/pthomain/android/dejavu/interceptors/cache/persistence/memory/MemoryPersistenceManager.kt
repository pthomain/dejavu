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
import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.configuration.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.BaseKeyValuePersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.file.FileNameSerialiser
import dev.pthomain.android.dejavu.interceptors.cache.persistence.file.FileNameSerialiser.Companion.SEPARATOR
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.CacheDataHolder
import java.util.*


/**
 * Provides a PersistenceManager implementation saving the responses in memory.
 * This will only provide cache for requests made during the lifecycle of the Application so
 * it is less useful and not the recommended solution. However, it can provide some level of
 * cache for applications with a strict "no persistence" policy. It uses a LruCache under the hood.
 *
 * This type of PersistenceManager would usually not benefit of encryption being used, so it
 * can be disabled to improve performance.
 *
 * @param hasher the class handling the request hashing for unicity
 * @param cacheConfiguration the global cache configuration
 * @param serialisationManagerFactory used for the serialisation/deserialisation of the cache entries
 * @param dateFactory class providing the time, for the purpose of testing
 * @param fileNameSerialiser a class that handles the serialisation of the cache metadata to a file name.
 */
class MemoryPersistenceManager<E> private constructor(hasher: Hasher,
                                                      cacheConfiguration: CacheConfiguration<E>,
                                                      dateFactory: (Long?) -> Date,
                                                      serialisationManagerFactory: SerialisationManager.Factory<E>,
                                                      fileNameSerialiser: FileNameSerialiser,
                                                      private val lruCache: LruCache<String, CacheDataHolder.Incomplete>,
                                                      disableEncryption: Boolean)
    : BaseKeyValuePersistenceManager<E>(
        hasher,
        cacheConfiguration,
        serialisationManagerFactory,
        dateFactory,
        fileNameSerialiser
) where E : Exception,
        E : NetworkErrorPredicate {

    override val serialisationManager = serialisationManagerFactory.create(disableEncryption = disableEncryption)

    /**
     * Returns an existing file name matching the given URL hash.
     *
     * @param hash the URL hash used as a unique key
     * @return the matching file name if present
     */
    override fun findEntryNameByHash(hash: String) =
            lruCache.snapshot()
                    .entries
                    .firstOrNull { entry ->
                        entry.key.startsWith(hash + SEPARATOR)
                    }?.key

    /**
     * Renames an entry
     *
     * @param oldName the old entry name
     * @param newName  the new entry name
     */
    override fun rename(oldName: String,
                        newName: String) {
        lruCache.put(newName, lruCache.get(oldName))
        delete(oldName)
    }

    /**
     * Saves an entry to the map
     *
     * @param key the file name
     * @param value the CacheDataHolder to serialise
     */
    override fun save(key: String,
                      value: CacheDataHolder.Incomplete) {
        lruCache.put(key, value)
    }

    /**
     * @return a map of all present entries
     */
    override fun list() = lruCache.snapshot()

    /**
     * Checks whether a given key is present in the cache
     *
     * @param key the given key
     * @return whether this entry exists in the map
     */
    override fun exists(key: String) = lruCache.get(key) != null

    /**
     * Deletes an entry by its given name
     *
     * @param key the name of the entry to delete
     */
    override fun delete(key: String) {
        lruCache.remove(key)
    }

    class Factory<E> internal constructor(private val hasher: Hasher,
                                          private val cacheConfiguration: CacheConfiguration<E>,
                                          private val dateFactory: (Long?) -> Date,
                                          private val fileNameSerialiser: FileNameSerialiser,
                                          private val serialisationManagerFactory: SerialisationManager.Factory<E>)
            where E : Exception,
                  E : NetworkErrorPredicate {

        fun create(maxSize: Int = 20,
                   disableEncryption: Boolean = true): PersistenceManager<E> =
                MemoryPersistenceManager(
                        hasher,
                        cacheConfiguration,
                        dateFactory,
                        serialisationManagerFactory,
                        fileNameSerialiser,
                        LruCache(maxSize),
                        disableEncryption
                )
    }

}