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
import dev.pthomain.android.dejavu.configuration.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.BasePersistenceManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.file.FileNameSerialiser.Companion.SEPARATOR
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
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
internal class FilePersistenceManager<E>(private val hasher: Hasher,
                                         private val fileFactory: (File, String) -> File,
                                         private val fileInputStreamFactory: (File) -> InputStream,
                                         private val fileOutputStreamFactory: (File) -> OutputStream,
                                         private val fileReader: (InputStream) -> ByteArray,
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
        E : NetworkErrorPredicate {

    init {
        cacheDirectory.mkdirs()
    }

    /**
     * Caches a given response.
     *
     * @param response the response to cache
     * @param previousCachedResponse the previously cached response if available for the purpose of replicating the previous cache settings for the new entry (i.e. compression and encryption)
     * @return whether or not the serialisation was successful
     */
    @Throws(Exception::class)
    override fun cache(response: ResponseWrapper<E>,
                       previousCachedResponse: ResponseWrapper<E>?) {
        serialise(response, previousCachedResponse)?.let { holder ->

            findFileByHash(holder.requestMetadata.urlHash)?.let {
                fileFactory(cacheDirectory, it).delete()
            }

            val name = fileNameSerialiser.serialise(holder)
            val file = fileFactory(cacheDirectory, name)

            fileOutputStreamFactory(file).useAndLogError(
                    {
                        it.write(holder.data)
                        it.flush()
                    },
                    logger
            )
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
            findFileByHash(requestMetadata.urlHash)?.let {
                val file = fileFactory(cacheDirectory, it)

                val data = fileInputStreamFactory(file).useAndLogError(
                        fileReader::invoke,
                        logger
                )

                fileNameSerialiser.deserialise(
                        instructionToken.requestMetadata,
                        it
                )?.copy(data = data)
            }

    private fun findFileByHash(hash: String) =
            cacheDirectory.list().firstOrNull { name ->
                name.startsWith(hash + SEPARATOR)
            }

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
        val classHash = typeToClear?.let { hasher.hash(it.name) }

        cacheDirectory.list()
                .map { it to fileNameSerialiser.deserialise(it) }
                .filter {
                    with(it.second) {
                        val isRightType = typeToClear == null || responseClassHash == classHash
                        val isRightExpiry = !clearStaleEntriesOnly || expiryDate <= now
                        isRightType && isRightExpiry
                    }
                }
                .map { fileFactory(cacheDirectory, it.first) }
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
    override fun invalidateIfNeeded(instructionToken: CacheToken): Boolean {
        if (instructionToken.instruction.operation.type.let { it == INVALIDATE || it == REFRESH }) {
            findFileByHash(instructionToken.requestMetadata.urlHash)?.also { oldName ->
                fileNameSerialiser
                        .deserialise(instructionToken.requestMetadata, oldName)
                        .copy(
                                expiryDate = 0L,
                                requestMetadata = instructionToken.requestMetadata
                        )
                        .let { invalidatedHolder ->
                            val newName = fileNameSerialiser.serialise(invalidatedHolder)
                            fileFactory(cacheDirectory, oldName).renameTo(fileFactory(cacheDirectory, newName))
                            true
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
            E : NetworkErrorPredicate {

        fun create(cacheDirectory: File = cacheConfiguration.context.cacheDir) =
                FilePersistenceManager(
                        hasher,
                        ::File,
                        { BufferedInputStream(FileInputStream(it)) },
                        { BufferedOutputStream(FileOutputStream(it)) },
                        { it.readBytes() },
                        cacheConfiguration,
                        serialisationManager,
                        dateFactory,
                        fileNameSerialiser,
                        cacheDirectory
                )

    }

}