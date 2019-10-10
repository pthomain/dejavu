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

package dev.pthomain.android.dejavu.interceptors.cache.persistence.file

import dev.pthomain.android.boilerplate.core.utils.io.useAndLogError
import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.configuration.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.BaseKeyValuePersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.file.FileNameSerialiser.Companion.SEPARATOR
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.CacheDataHolder.Incomplete
import java.io.*
import java.util.*


/**
 * Provides a PersistenceManager implementation saving the responses to the given directory.
 * This would be slightly less performant than the database implementation with a large number of entries.
 *
 * Be careful to encrypt the data if you change this directory to a publicly readable directory,
 * see CacheConfiguration.Builder().encryptByDefault().
 *
 * @param hasher the class handling the request hashing for unicity
 * @param fileFactory a factory that returns a File for a given parent directory and file name
 * @param fileInputStreamFactory a factory that returns an InputStream for a given File
 * @param fileOutputStreamFactory a factory that returns an OutputStream for a given File
 * @param fileReader a factory that returns a ByteArray for a given file input stream
 * @param cacheConfiguration the global cache configuration
 * @param serialisationManagerFactory used for the serialisation/deserialisation of the cache entries
 * @param dateFactory class providing the time, for the purpose of testing
 * @param fileNameSerialiser a class that handles the serialisation of the cache metadata to a file name.
 * @param cacheDirectory which directory to use to persist the response (use cache dir by default)
 */
class FilePersistenceManager<E> private constructor(hasher: Hasher,
                                                    private val fileFactory: (File, String) -> File,
                                                    private val fileInputStreamFactory: (File) -> InputStream,
                                                    private val fileOutputStreamFactory: (File) -> OutputStream,
                                                    private val fileReader: (InputStream) -> ByteArray,
                                                    cacheConfiguration: CacheConfiguration<E>,
                                                    serialisationManagerFactory: SerialisationManager.Factory<E>,
                                                    dateFactory: (Long?) -> Date,
                                                    fileNameSerialiser: FileNameSerialiser,
                                                    val cacheDirectory: File)
    : BaseKeyValuePersistenceManager<E>(
        hasher,
        cacheConfiguration,
        serialisationManagerFactory,
        dateFactory,
        fileNameSerialiser
) where E : Exception,
        E : NetworkErrorPredicate {

    override val serialisationManager = serialisationManagerFactory.create(isFilePersistence = true)

    init {
        cacheDirectory.mkdirs()
    }

    /**
     * Returns an existing entry name matching the given URL hash.
     *
     * @param hash the URL hash used as a unique key
     * @return the matching file name if present
     */
    override fun findEntryNameByHash(hash: String) =
            cacheDirectory.list().firstOrNull { name ->
                name.startsWith(hash + SEPARATOR)
            }

    /**
     * Renames an entry
     *
     * @param oldName the old entry name
     * @param newName  the new entry name
     */
    override fun rename(oldName: String,
                        newName: String) {
        fileFactory(cacheDirectory, oldName)
                .renameTo(fileFactory(cacheDirectory, newName))
    }

    /**
     * Saves a file to disk
     *
     * @param key the file name
     * @param value the CacheDataHolder to serialise
     */
    override fun save(key: String,
                      value: Incomplete) {
        val file = fileFactory(cacheDirectory, key)
        fileOutputStreamFactory(file).useAndLogError(
                {
                    it.write(value.data)
                    it.flush()
                },
                logger
        )
    }

    /**
     * @return a map of all present entries
     */
    override fun list() =
            cacheDirectory.list()
                    .associate { it to get(it) }

    /**
     * Checks whether a given key is present in the cache
     *
     * @param key the given key
     * @return whether this entry exists in the map
     */
    override fun exists(key: String) =
            list().keys.contains(key)

    /**
     * Deletes an entry by its given name
     *
     * @param key the name of the entry to delete
     */
    override fun delete(key: String) {
        fileFactory(cacheDirectory, key).delete()
    }

    class Factory<E> internal constructor(private val hasher: Hasher,
                                          private val cacheConfiguration: CacheConfiguration<E>,
                                          private val serialisationManagerFactory: SerialisationManager.Factory<E>,
                                          private val dateFactory: (Long?) -> Date,
                                          private val fileNameSerialiser: FileNameSerialiser
    ) where E : Exception,
            E : NetworkErrorPredicate {

        fun create(cacheDirectory: File = cacheConfiguration.context.cacheDir): PersistenceManager<E> =
                FilePersistenceManager(
                        hasher,
                        ::File,
                        { BufferedInputStream(FileInputStream(it)) },
                        { BufferedOutputStream(FileOutputStream(it)) },
                        { it.readBytes() },
                        cacheConfiguration,
                        serialisationManagerFactory,
                        dateFactory,
                        fileNameSerialiser,
                        cacheDirectory
                )

    }

}