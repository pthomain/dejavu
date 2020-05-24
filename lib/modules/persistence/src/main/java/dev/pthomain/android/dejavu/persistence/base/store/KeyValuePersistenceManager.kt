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

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.CacheInstruction
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear.Scope.ALL
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.persistence.Persisted.Serialised
import dev.pthomain.android.dejavu.persistence.base.BasePersistenceManager
import dev.pthomain.android.dejavu.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.serialisation.SerialisationException
import dev.pthomain.android.dejavu.serialisation.SerialisationManager
import java.util.*

/**
 * Provides a PersistenceManager implementation saving the responses to the given directory.
 * This would be slightly less performant than the database implementation with a large number of entries.
 *
 * Be careful to encrypt the data if you change this directory to a publicly readable directory,
 * see DejaVu.Configuration.Builder().encryptByDefault().
 *
 * @param dateFactory class providing the time, for the purpose of testing
 * @param keySerialiser a class that handles the serialisation of the cache metadata to a file name.
 * @param store the KeyValueStore holding the data
 * @param serialisationManager used for the serialisation/deserialisation of the cache entries
 */
class KeyValuePersistenceManager(
        dateFactory: (Long?) -> Date,
        logger: Logger,
        private val keySerialiser: KeySerialiser,
        private val store: KeyValueStore<String, String, Serialised>,
        serialisationManager: SerialisationManager,
        override val decorator: SerialisationDecorator? = null
) : BasePersistenceManager(
        logger,
        serialisationManager,
        dateFactory
) {

    /**
     * Caches a given response.
     *
     * @param responseWrapper the response to cache
     * @throws SerialisationException in case the serialisation failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> put(response: Response<R, Cache>) {
        val cacheToken = response.cacheToken

        store.findPartialKey(cacheToken.instruction.requestMetadata.requestHash)
                ?.let(store::delete)

        with(cacheToken.instruction.requestMetadata) {
            store.save(
                    keySerialiser.serialise(cacheToken),
                    Serialised(
                            requestHash,
                            classHash,
                            cacheToken.requestDate,
                            cacheToken.expiryDate!!,
                            cacheToken.instruction.operation.serialisation,
                            serialise(response)
                    )
            )
        }
    }

    /**
     * Returns the cached data as a CacheDataHolder object.
     *
     * @param requestMetadata the associated request metadata
     *
     * @return the cached data as a CacheDataHolder
     */
    override fun <R : Any> get(requestMetadata: HashedRequestMetadata<R>): Serialised? =
            store.findPartialKey(requestMetadata.requestHash)?.let(store::get)

    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param requestMetadata the request's metadata
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> clearCache(
            requestMetadata: HashedRequestMetadata<R>,
            operation: Clear
    ) {
        store.values()
                .map { it.key to keySerialiser.deserialise(it.key) }
                .filter {
                    with(it.second) {
                        val isRightExpiry = !operation.clearStaleEntriesOnly || expiryDate.before(dateFactory(null))

                        if (operation.scope == ALL) isRightExpiry
                        else isRightExpiry && requestHash == requestMetadata.requestHash
                    }
                }
                .forEach { store.delete(it.first) }
    }

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE).
     *
     * @param requestMetadata the request's metadata
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    override fun <R : Any> forceInvalidation(token: RequestToken<*, R>): Boolean {
        get(token.instruction.requestMetadata)?.also { serialised ->
            if (serialised.expiryDate.time != 0L) {

                val oldResponseToken = ResponseToken(
                        CacheInstruction(Cache(), token.instruction.requestMetadata),
                        token.status,
                        serialised.requestDate,
                        serialised.expiryDate
                )

                val newResponseToken = oldResponseToken.copy(
                        expiryDate = dateFactory(0L)
                )

                store.rename(
                        keySerialiser.serialise(oldResponseToken),
                        keySerialiser.serialise(newResponseToken)
                )

                return true
            }
        }
        return false
    }
}
