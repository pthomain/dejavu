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

import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Type.INVALIDATE
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Type.REFRESH
import dev.pthomain.android.dejavu.configuration.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.persistence.file.FileNameSerialiser
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationException
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.CacheDataHolder
import java.util.*

/**
 * Provides a PersistenceManager implementation saving the responses to the given directory.
 * This would be slightly less performant than the database implementation with a large number of entries.
 *
 * Be careful to encrypt the data if you change this directory to a publicly readable directory,
 * see CacheConfiguration.Builder().encryptByDefault().
 *
 * @param hasher the class handling the request hashing for unicity
 * @param cacheConfiguration the global cache configuration
 * @param serialisationManagerFactory used for the serialisation/deserialisation of the cache entries
 * @param dateFactory class providing the time, for the purpose of testing
 * @param fileNameSerialiser a class that handles the serialisation of the cache metadata to a file name.
 */
abstract class BaseKeyValuePersistenceManager<E> internal constructor(private val hasher: Hasher,
                                                                      cacheConfiguration: CacheConfiguration<E>,
                                                                      serialisationManagerFactory: SerialisationManager.Factory<E>,
                                                                      dateFactory: (Long?) -> Date,
                                                                      private val fileNameSerialiser: FileNameSerialiser)
    : BasePersistenceManager<E>(
        cacheConfiguration,
        serialisationManagerFactory,
        dateFactory
), KeyValueStore<String, CacheDataHolder.Incomplete>
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * Returns an existing entry name matching the given URL hash.
     *
     * @param hash the URL hash used as a unique key
     * @return the matching file name if present
     */
    abstract fun findEntryNameByHash(hash: String): String?

    /**
     * Converts a given key to an incomplete CacheDataHolder
     *
     * @param key the entry's key
     * @return the incomplete CacheDataHolder if present in cache
     */
    final override fun get(key: String) =
            fileNameSerialiser.deserialise(key)

    /**
     * Caches a given response.
     *
     * @param responseWrapper the response to cache
     * @param previousCachedResponse the previously cached response if available for the purpose of replicating the previous cache settings for the new entry (i.e. compression and encryption)
     * @throws SerialisationException in case the serialisation failed
     */
    @Throws(SerialisationException::class)
    final override fun cache(responseWrapper: ResponseWrapper<E>,
                             previousCachedResponse: ResponseWrapper<E>?) {
        serialise(responseWrapper, previousCachedResponse)?.let { holder ->

            findEntryNameByHash(holder.requestMetadata.urlHash)?.let {
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
    final override fun getCacheDataHolder(instructionToken: CacheToken,
                                          requestMetadata: RequestMetadata.Hashed) =
            findEntryNameByHash(requestMetadata.urlHash)?.let {
                fileNameSerialiser.deserialise(
                        instructionToken.requestMetadata,
                        it
                ).copy(data = get(it).data)
            }

    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param typeToClear type of entries to clear (or all the entries if this parameter is null)
     * @param clearStaleEntriesOnly only clear STALE entries if set to true (or all otherwise)
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    final override fun clearCache(typeToClear: Class<*>?,
                                  clearStaleEntriesOnly: Boolean) {
        val now = dateFactory(null).time
        val classHash = typeToClear?.let { hasher.hash(it.name) }

        list().map { it.key to fileNameSerialiser.deserialise(it.key) }
                .filter {
                    with(it.second) {
                        val isRightType = typeToClear == null || responseClassHash == classHash
                        val isRightExpiry = !clearStaleEntriesOnly || expiryDate <= now
                        isRightType && isRightExpiry
                    }
                }
                .forEach { delete(it.first) }
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
    final override fun invalidateIfNeeded(instructionToken: CacheToken): Boolean {
        if (instructionToken.instruction.operation.type.let { it == INVALIDATE || it == REFRESH }) {
            findEntryNameByHash(instructionToken.requestMetadata.urlHash)?.also { oldName ->
                fileNameSerialiser
                        .deserialise(instructionToken.requestMetadata, oldName)
                        .copy(
                                expiryDate = 0L,
                                requestMetadata = instructionToken.requestMetadata
                        )
                        .let { invalidatedHolder ->
                            val newName = fileNameSerialiser.serialise(invalidatedHolder)
                            rename(oldName, newName)
                            true
                        }
            }
        }
        return false
    }

    /**
     * Renames an entry
     *
     * @param oldName the old entry name
     * @param newName  the new entry name
     */
    abstract fun rename(oldName: String, newName: String)
}
