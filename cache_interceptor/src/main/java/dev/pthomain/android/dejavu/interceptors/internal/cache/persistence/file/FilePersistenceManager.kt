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

package dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.file

import dev.pthomain.android.boilerplate.core.utils.io.useAndLogError
import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Type.INVALIDATE
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Type.REFRESH
import dev.pthomain.android.dejavu.configuration.NetworkErrorProvider
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.BasePersistenceManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.file.FileNameSerialiser.Companion.SEPARATOR
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.token.CacheToken
import dev.pthomain.android.dejavu.response.ResponseWrapper
import java.io.*
import java.util.*


/**
 * Provides a PersistenceManager implementation saving the responses to the give repository.
 * This would be less performant than the database implementation.
 *
 * Be careful to encrypt the data if you change this directory to a publicly readable directory,
 * see CacheConfiguration.Builder().encryptByDefault().
 *
 * @param context the application context
 * @param cacheDirectory which directory to use to persist the response (use cache dir by default)
 */
class FilePersistenceManager<E> internal constructor(private val hasher: Hasher,
                                                     cacheConfiguration: CacheConfiguration<E>,
                                                     serialisationManager: SerialisationManager<E>,
                                                     dateFactory: (Long?) -> Date,
                                                     private val fileNameSerialiser: FileNameSerialiser,
                                                     private val cacheDirectory: File)
    : BasePersistenceManager<E>(
        cacheConfiguration,
        serialisationManager,
        dateFactory
) where E : Exception,
        E : NetworkErrorProvider {

    init {
        cacheDirectory.mkdirs()
    }

    /**
     * Caches a given response.
     *
     * @param response the response to cache
     * @param previousCachedResponse the previously cached response if available for the purpose of replicating the previous cache settings for the new entry (i.e. compression and encryption)
     */
    override fun cache(response: ResponseWrapper<E>,
                       previousCachedResponse: ResponseWrapper<E>?) {
        with(serialise(response, previousCachedResponse)) {
            if (this != null) {
                val name = fileNameSerialiser.serialise(this)
                val file = File(cacheDirectory, name)

                BufferedOutputStream(FileOutputStream(file)).useAndLogError(
                        {
                            it.write(data)
                            it.flush()
                        },
                        logger
                )
            }
        }
    }

    /**
     * Returns the cached data as a CacheDataHolder object.
     *
     * @param instructionToken the instruction CacheToken containing the description of the desired entry.
     * @param requestMetadata the associated request metadata
     * @param start the time at which the operation started in order to calculate the time the operation took.
     *
     * @return the cached data as a CacheDataHolder
     */
    override fun getCacheDataHolder(instructionToken: CacheToken,
                                    requestMetadata: RequestMetadata.Hashed,
                                    start: Long) =
            findFileByHash(requestMetadata.urlHash)?.let {
                val file = File(cacheDirectory, it)

                val data = BufferedInputStream(FileInputStream(file)).useAndLogError(
                        { it.readBytes() },
                        logger
                )

                fileNameSerialiser.deserialise(
                        instructionToken.requestMetadata,
                        it
                )?.copy(data = data)
            }

    private fun findFileByHash(hash: String) =
            cacheDirectory.list { _, name ->
                name.startsWith(hash + SEPARATOR)
            }.firstOrNull()

    /**
     * Clears the entries of a certain type as passed by the typeToClear argument (or all entries otherwise).
     * Both parameters work in conjunction to form an intersection of entries to be cleared.
     *
     * @param typeToClear type of entries to clear (or all the entries if this parameter is null)
     * @param clearStaleEntriesOnly only clear STALE entries if set to true (or all otherwise)
     */
    override fun clearCache(typeToClear: Class<*>?,
                            clearStaleEntriesOnly: Boolean) {
        val now = dateFactory(null).time

        cacheDirectory.list()
                .map { it to fileNameSerialiser.deserialise(null, it) }
                .filter {
                    val cacheDataHolder = it.second
                    if (cacheDataHolder != null) {
                        val classHash = typeToClear?.let { hasher.hash(it.name) }
                        val isRightType = typeToClear == null || cacheDataHolder.responseClassName == classHash
                        val isRightExpiry = !clearStaleEntriesOnly || cacheDataHolder.expiryDate <= now
                        isRightType && isRightExpiry
                    } else false
                }
                .map { File(cacheDirectory, it.first) }
                .forEach { it.delete() }
    }

    /**
     * Invalidates the cached data (by setting the expiry date in the past, making the data STALE)
     *
     * @param instructionToken the INVALIDATE instruction token for the desired entry.
     * @param key the key of the entry to invalidate
     *
     * @return a Boolean indicating whether the data marked for invalidation was found or not
     */
    override fun checkInvalidation(instructionToken: CacheToken): Boolean {
        if (instructionToken.instruction.operation.type.let { it == INVALIDATE || it == REFRESH }) {
            findFileByHash(instructionToken.requestMetadata.urlHash)?.also { oldName ->
                fileNameSerialiser
                        .deserialise(instructionToken.requestMetadata, oldName)
                        ?.copy(
                                expiryDate = 0L,
                                requestMetadata = instructionToken.requestMetadata
                        )
                        ?.also { invalidatedHolder ->
                            val newName = fileNameSerialiser.serialise(invalidatedHolder)
                            File(cacheDirectory, oldName).renameTo(File(cacheDirectory, newName))
                            return true
                        }
            }
        }
        return false
    }

    class Factory<E> internal constructor(private val hasher: Hasher,
                                          private val cacheConfiguration: CacheConfiguration<E>,
                                          private val serialisationManager: SerialisationManager<E>,
                                          private val dateFactory: (Long?) -> Date,
                                          private val fileNameSerialiser: FileNameSerialiser
    ) where E : Exception,
            E : NetworkErrorProvider {

        fun create(cacheDirectory: File = cacheConfiguration.context.cacheDir) =
                FilePersistenceManager(
                        hasher,
                        cacheConfiguration,
                        serialisationManager,
                        dateFactory,
                        fileNameSerialiser,
                        cacheDirectory
                )

    }

}