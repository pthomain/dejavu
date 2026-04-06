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

package dev.pthomain.android.dejavu.persistence.sqlite

import dev.pthomain.android.dejavu.utils.Logger
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.HashedRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear.Scope
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.di.DateFactory
import dev.pthomain.android.dejavu.persistence.Persisted.Serialised
import dev.pthomain.android.dejavu.persistence.base.BasePersistenceManager
import dev.pthomain.android.dejavu.persistence.sqlite.dao.CacheDao
import dev.pthomain.android.dejavu.persistence.sqlite.entity.CacheEntry
import dev.pthomain.android.dejavu.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.serialisation.SerialisationException
import dev.pthomain.android.dejavu.serialisation.SerialisationManager
import kotlinx.coroutines.runBlocking

/**
 * Provides a PersistenceManager implementation saving the responses to a Room database.
 *
 * @param cacheDao the Room DAO for cache operations
 * @param serialisationManager used for the serialisation/deserialisation of the cache entries
 * @param dateFactory class providing the time, for the purpose of testing
 */
class DatabasePersistenceManager internal constructor(
    private val cacheDao: CacheDao,
    logger: Logger,
    serialisationManager: SerialisationManager,
    dateFactory: DateFactory
) : BasePersistenceManager(
    logger,
    serialisationManager,
    dateFactory
) {

    override val decorator: SerialisationDecorator? = null

    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param operation the Clear operation
     * @param requestMetadata the request's metadata
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> clearCache(
        requestMetadata: HashedRequestMetadata<R>,
        operation: Clear
    ) {
        runBlocking {
            if (operation.clearStaleEntriesOnly) {
                cacheDao.deleteExpired(dateFactory(null).time)
            } else {
                when (operation.scope) {
                    Scope.REQUEST -> cacheDao.deleteByHash(requestMetadata.requestHash)
                    Scope.CLASS -> cacheDao.deleteByClass(requestMetadata.classHash)
                    Scope.ALL -> cacheDao.deleteAll()
                }
            }
        }

        val entryType = requestMetadata.responseClass.simpleName
        if (operation.clearStaleEntriesOnly) {
            logger.d(this, "Deleted old $entryType entries from cache")
        } else {
            logger.d(this, "Deleted all existing $entryType entries from cache")
        }
    }

    /**
     * Returns the cached data as a CacheDataHolder object.
     *
     * @param requestMetadata the associated request metadata
     *
     * @return the cached data as a CacheDataHolder
     * @throws SerialisationException in case the deserialisation failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> get(requestMetadata: HashedRequestMetadata<R>): Serialised? {
        val simpleName = requestMetadata.responseClass.simpleName

        return runBlocking {
            cacheDao.getByHash(requestMetadata.requestHash)
        }?.let { entry ->
            logger.d(this, "Found a cached $simpleName")

            Serialised(
                entry.requestHash,
                entry.classHash,
                dateFactory(entry.cacheDate),
                dateFactory(entry.expiryDate),
                entry.serialisation,
                entry.data
            )
        } ?: run {
            logger.d(this, "Found no cached $simpleName")
            null
        }
    }

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE).
     *
     * @param requestMetadata the request's metadata
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    override fun <R : Any> forceInvalidation(token: RequestToken<*, R>): Boolean {
        val requestMetadata = token.instruction.requestMetadata
        val results = runBlocking {
            cacheDao.updateExpiryDate(requestMetadata.requestHash, 0L)
        }

        val foundIt = results > 0

        logger.d(
            this,
            "Invalidating cache for ${requestMetadata.responseClass.simpleName}: ${if (foundIt) "done" else "nothing found"}"
        )

        return foundIt
    }

    /**
     * Caches a given response.
     *
     * @param responseWrapper the response to cache
     * @throws SerialisationException in case the serialisation failed
     */
    @Throws(SerialisationException::class)
    override fun <R : Any> put(response: Response<R, Cache>) {
        val serialised = serialise(response)

        val cacheToken = response.cacheToken
        val requestMetadata = cacheToken.instruction.requestMetadata

        val entry = CacheEntry(
            requestHash = requestMetadata.requestHash,
            classHash = requestMetadata.classHash,
            cacheDate = cacheToken.requestDate.time,
            expiryDate = cacheToken.expiryDate!!.time,
            serialisation = cacheToken.instruction.operation.serialisation,
            data = serialised
        )

        try {
            runBlocking {
                cacheDao.insert(entry)
            }
        } catch (e: Exception) {
            throw SerialisationException("Could not save the response to database", e)
        }
    }
}
