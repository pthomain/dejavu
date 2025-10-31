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

package dev.pthomain.android.dejavu.persistence

import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.persistence.Persisted.Deserialised
import dev.pthomain.android.dejavu.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.serialisation.SerialisationException
import org.koin.core.module.Module

interface PersistenceManager {

    /**
     * Returns a cached entry if available
     *
     * @param instructionToken the request's instruction token
     *
     * @return a cached entry if available, or null otherwise
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    fun <R : Any> get(cacheToken: RequestToken<Cache, R>): Deserialised<R>?

    /**
     * Caches a given response.
     *
     * @param responseWrapper the response to cache
     * @throws SerialisationException in case the serialisation failed
     */
    @Throws(SerialisationException::class)
    fun <R : Any> put(response: Response<R, Cache>)

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE).
     *
     * @param requestMetadata the request's metadata
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    fun <R : Any> forceInvalidation(token: RequestToken<*, R>): Boolean

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     * if the CachePriority requires it.
     *
     * @param operation the request's operation
     * @param requestMetadata the request's metadata
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    fun <R : Any> invalidateIfNeeded(cacheToken: RequestToken<Cache, R>): Boolean

    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param operation the Clear operation
     * @param requestMetadata the request's metadata
     */
    fun <R : Any> clearCache(
            requestMetadata: HashedRequestMetadata<R>,
            operation: Clear = Clear()
    )

    interface ModuleProvider {
        val modules: List<Module>
        val decorators: List<SerialisationDecorator>
    }

}