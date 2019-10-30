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

import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.configuration.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Clear
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.file.FileStore
import dev.pthomain.android.dejavu.interceptors.cache.persistence.memory.MemoryStore
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.FileNameSerialiser
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationException
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager.Factory.Type.FILE
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager.Factory.Type.MEMORY
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.utils.Utils.invalidatesExistingData
import java.util.*

/**
 * Provides a PersistenceManager implementation saving the responses to the given directory.
 * This would be slightly less performant than the database implementation with a large number of entries.
 *
 * Be careful to encrypt the data if you change this directory to a publicly readable directory,
 * see DejaVuConfiguration.Builder().encryptByDefault().
 *
 * @param dejaVuConfiguration the global cache configuration
 * @param dateFactory class providing the time, for the purpose of testing
 * @param fileNameSerialiser a class that handles the serialisation of the cache metadata to a file name.
 * @param store the KeyValueStore holding the data
 * @param serialisationManager used for the serialisation/deserialisation of the cache entries
 */
class KeyValuePersistenceManager<E> internal constructor(dejaVuConfiguration: DejaVuConfiguration<E>,
                                                         dateFactory: (Long?) -> Date,
                                                         private val fileNameSerialiser: FileNameSerialiser,
                                                         private val store: KeyValueStore<String, String, CacheDataHolder.Incomplete>,
                                                         serialisationManager: SerialisationManager<E>)
    : BasePersistenceManager<E>(
        dejaVuConfiguration,
        serialisationManager,
        dateFactory
), KeyValueStore<String, String, CacheDataHolder.Incomplete> by store
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * Caches a given response.
     *
     * @param responseWrapper the response to cache
     * @param previousCachedResponse the previously cached response if available for the purpose of replicating the previous cache settings for the new entry (i.e. compression and encryption)
     * @throws SerialisationException in case the serialisation failed
     */
    @Throws(SerialisationException::class)
    override fun cache(responseWrapper: ResponseWrapper<E>,
                       previousCachedResponse: ResponseWrapper<E>?) {
        serialise(responseWrapper, previousCachedResponse).let { holder ->

            findPartialKey(holder.requestMetadata.requestHash)?.let {
                delete(it)
            }

            val name = fileNameSerialiser.serialise(holder)
            save(name, holder.incomplete)
        }
    }

    /**
     * Returns the cached data as a CacheDataHolder object.
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     * @param requestMetadata the associated request metadata
     *
     * @return the cached data as a CacheDataHolder
     */
    override fun getCacheDataHolder(instructionToken: CacheToken,
                                    requestMetadata: RequestMetadata.Hashed) =
            findPartialKey(requestMetadata.requestHash)
                    ?.let { get(it) }

    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    override fun clearCache(instructionToken: CacheToken) {
        val now = dateFactory(null).time
        with(instructionToken.instruction) {
            if (operation !is Clear) throw SerialisationException("Wrong operation type: $operation")

            values().map { it.key to fileNameSerialiser.deserialise(it.key) }
                    .filter {
                        val holder = it.second

                        val isClearAll = requestMetadata.responseClass == Any::class.java
                        val isRightType = isClearAll || holder.responseClassHash == requestMetadata.classHash
                        val isRightRequest = !operation.useRequestParameters || holder.requestHash == requestMetadata.requestHash
                        val isRightExpiry = !operation.clearStaleEntriesOnly || holder.expiryDate <= now

                        isRightType && isRightRequest && isRightExpiry
                    }
                    .forEach { delete(it.first) }
        }
    }

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     *
     * @param instructionToken the INVALIDATE instruction token for the desired entry.
     * @param key the key of the entry to invalidate
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    override fun invalidateIfNeeded(instructionToken: CacheToken): Boolean {
        val operation = instructionToken.instruction.operation

        if (operation.invalidatesExistingData()) {
            val requestMetadata = instructionToken.instruction.requestMetadata

            findPartialKey(requestMetadata.requestHash)?.also { oldName ->
                fileNameSerialiser.deserialise(requestMetadata, oldName).let {
                    if (it.expiryDate != 0L) {
                        val newName = fileNameSerialiser.serialise(it.copy(expiryDate = 0L))
                        rename(oldName, newName)
                    }
                    true
                }
            }
        }
        return false
    }

    class FileFactory<E> internal constructor(private val fileStoreFactory: FileStore.Factory<E>,
                                              private val serialisationManagerFactory: SerialisationManager.Factory<E>,
                                              private val dejaVuConfiguration: DejaVuConfiguration<E>,
                                              private val dateFactory: (Long?) -> Date,
                                              private val fileNameSerialiser: FileNameSerialiser)
            where E : Exception,
                  E : NetworkErrorPredicate {
        fun create(cacheDirectory: java.io.File = dejaVuConfiguration.context.cacheDir): PersistenceManager<E> {
            return KeyValuePersistenceManager(
                    dejaVuConfiguration,
                    dateFactory,
                    fileNameSerialiser,
                    fileStoreFactory.create(cacheDirectory),
                    serialisationManagerFactory.create(FILE)
            )
        }
    }

    class MemoryFactory<E> internal constructor(private val memoryStoreFactory: MemoryStore.Factory,
                                                private val serialisationManagerFactory: SerialisationManager.Factory<E>,
                                                private val dejaVuConfiguration: DejaVuConfiguration<E>,
                                                private val dateFactory: (Long?) -> Date,
                                                private val fileNameSerialiser: FileNameSerialiser)
            where E : Exception,
                  E : NetworkErrorPredicate {
        fun create(maxEntries: Int = 20,
                   disableEncryption: Boolean = false): PersistenceManager<E> =
                KeyValuePersistenceManager(
                        dejaVuConfiguration,
                        dateFactory,
                        fileNameSerialiser,
                        memoryStoreFactory.create(maxEntries),
                        serialisationManagerFactory.create(MEMORY, disableEncryption)
                )
    }
}
