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

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.getCacheStatus
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear.Scope.REQUEST
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.persistence.Persisted.Deserialised
import dev.pthomain.android.dejavu.persistence.Persisted.Serialised
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.dejavu.serialisation.SerialisationException
import dev.pthomain.android.dejavu.serialisation.SerialisationManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Provides a skeletal implementation of PersistenceManager
 *
 * @param serialisationManager used for the serialisation/deserialisation of the cache entries
 * @param dateFactory class providing the time, for the purpose of testing
 */
abstract class BasePersistenceManager(
        protected val logger: Logger,
        private val serialisationManager: SerialisationManager,
        protected val dateFactory: (Long?) -> Date
) : PersistenceManager {

    private val dateFormat = SimpleDateFormat("dd MMM HH:mm:s", Locale.UK)

    /**
     * Serialises the response and generates all the associated metadata for caching.
     *
     * @param response the response to cache.
     *
     * @return a model containing the serialised data along with the calculated metadata to use for caching it
     */
    protected fun <R : Any> serialise(response: Response<R, Cache>) =
            serialisationManager.serialise(
                    response,
                    decorator
            ).also { logger.d(this, "Caching ${response::class.java.simpleName}") }

    /**
     * Returns a cached entry if available
     *
     * @param instructionToken the request's instruction token
     *
     * @return a cached entry if available, or null otherwise
     * @throws SerialisationException in case the deserialisation failed
     */
    final override fun <R : Any> get(cacheToken: RequestToken<Cache, R>): Deserialised<R>? {
        val requestMetadata = cacheToken.instruction.requestMetadata
        logger.d(this, "Checking for cached ${requestMetadata.responseClass.simpleName}")

        invalidateIfNeeded(cacheToken)

        val persisted = get(requestMetadata)

        return persisted?.run {
            if (requestMetadata.classHash != classHash) {
                logger.e(this, "The class hash for the given request did not match, clearing entry.")
                clearCache(requestMetadata)
                null
            } else deserialise(cacheToken, persisted)
        }
    }

    /**
     * Returns the cached data as a CacheDataHolder object.
     *
     * @param requestMetadata the associated request metadata
     *
     * @return the cached data as a CacheDataHolder
     */
    protected abstract fun <R : Any> get(requestMetadata: HashedRequestMetadata<R>): Serialised?

    /**
     * Deserialises the cached data
     *
     * @param instructionToken the request's instruction token
     * @param cacheDate the Date at which the data was cached
     * @param expiryDate the Date at which the data would become STALE
     * @param localData the cached data
     *
     * @return a ResponseWrapper containing the deserialised data or null if the operation failed.
     */
    private fun <R : Any> deserialise(
            cacheToken: CacheToken<Cache, R>,
            persisted: Serialised
    ): Deserialised<R>? {
        val instruction = cacheToken.instruction
        val requestMetadata = instruction.requestMetadata
        val simpleName = requestMetadata.responseClass.simpleName

        return try {
            with(persisted) {
                serialisationManager.deserialise(
                        cacheToken.instruction,
                        this,
                        decorator
                ).let { response ->
                    val cacheStatus = dateFactory.getCacheStatus(expiryDate)
                    val formattedExpiry = dateFormat.format(expiryDate)
                    logger.d(
                            this,
                            "Found cached $simpleName, status: $cacheStatus cached until $formattedExpiry"
                    )

                    with(persisted) {
                        Deserialised(
                                requestHash,
                                classHash,
                                requestDate,
                                expiryDate,
                                serialisation,
                                response
                        )
                    }
                }
            }
        } catch (e: SerialisationException) {
            logger.e(this, "Could not deserialise $simpleName: clearing the entry")
            clearCache(requestMetadata, Clear(REQUEST))
            null
        }
    }

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     * only if the CachePriority requires it.
     *
     * @param operation the request's operation
     * @param requestMetadata the request's metadata
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    final override fun <R : Any> invalidateIfNeeded(cacheToken: RequestToken<Cache, R>) =
            with(cacheToken.instruction) {
                if (operation.priority.behaviour.isInvalidate()) {
                    forceInvalidation(cacheToken)
                } else false
            }

}
