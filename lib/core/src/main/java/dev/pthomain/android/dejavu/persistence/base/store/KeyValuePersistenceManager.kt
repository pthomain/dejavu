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

package dev.pthomain.android.dejavu.persistence.base.store

import dev.pthomain.android.dejavu.DejaVu.Configuration
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.ValidRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.persistence.base.BasePersistenceManager
import dev.pthomain.android.dejavu.persistence.base.CacheDataHolder
import dev.pthomain.android.dejavu.serialisation.FileNameSerialiser
import dev.pthomain.android.dejavu.serialisation.SerialisationException
import dev.pthomain.android.dejavu.serialisation.SerialisationManager
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import java.util.*

/**
 * Provides a PersistenceManager implementation saving the responses to the given directory.
 * This would be slightly less performant than the database implementation with a large number of entries.
 *
 * Be careful to encrypt the data if you change this directory to a publicly readable directory,
 * see DejaVu.Configuration.Builder().encryptByDefault().
 *
 * @param configuration the global cache configuration
 * @param dateFactory class providing the time, for the purpose of testing
 * @param fileNameSerialiser a class that handles the serialisation of the cache metadata to a file name.
 * @param store the KeyValueStore holding the data
 * @param serialisationManager used for the serialisation/deserialisation of the cache entries
 */
class KeyValuePersistenceManager<E>(
        configuration: Configuration<E>,
        dateFactory: (Long?) -> Date,
        private val fileNameSerialiser: FileNameSerialiser,
        private val store: KeyValueStore<String, String, CacheDataHolder.Incomplete>,
        serialisationManager: SerialisationManager<E>
) : BasePersistenceManager<E>(
        configuration,
        serialisationManager,
        dateFactory
), KeyValueStore<String, String, CacheDataHolder.Incomplete> by store
        where E : Throwable,
              E : NetworkErrorPredicate {

    /**
     * Caches a given response.
     *
     * @param responseWrapper the response to cache
     * @throws SerialisationException in case the serialisation failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> cache(responseWrapper: Response<R, Cache>) {
        serialise(responseWrapper).let { holder ->

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
     * @param requestMetadata the associated request metadata
     *
     * @return the cached data as a CacheDataHolder
     */
    override fun <R> getCacheDataHolder(requestMetadata: HashedRequestMetadata<R>) =
            findPartialKey(requestMetadata.requestHash)?.let(::get)

    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param requestMetadata the request's metadata
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    override fun <R> clearCache(operation: Clear,
                                requestMetadata: ValidRequestMetadata<R>) {
        val now = dateFactory(null).time
        values().map { it.key to fileNameSerialiser.deserialise(it.key) }
                .filter {
                    val holder = it.second

                    val isClearAll = requestMetadata.responseClass == Any::class.java
                    val isRightType = isClearAll || holder.responseClassHash == requestMetadata.classHash
                    val isRightRequest = holder.requestHash == requestMetadata.requestHash
                    val isRightExpiry = !operation.clearStaleEntriesOnly || holder.expiryDate <= now

                    isRightType && isRightRequest && isRightExpiry
                }
                .forEach { delete(it.first) }
    }

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE).
     *
     * @param requestMetadata the request's metadata
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    override fun <R> forceInvalidation(requestMetadata: ValidRequestMetadata<R>): Boolean {
        findPartialKey(requestMetadata.requestHash)?.also { oldName ->
            fileNameSerialiser.deserialise(requestMetadata, oldName).let {
                if (it.expiryDate != 0L) {
                    val newName = fileNameSerialiser.serialise(it.copy(expiryDate = 0L))
                    rename(oldName, newName)
                    return true
                }
            }
        }
        return false
    }

}
