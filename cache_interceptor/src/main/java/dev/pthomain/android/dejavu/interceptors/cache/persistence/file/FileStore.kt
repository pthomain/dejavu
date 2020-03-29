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

package dev.pthomain.android.dejavu.interceptors.cache.persistence.file

import dev.pthomain.android.boilerplate.core.utils.io.useAndLogError
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.CacheDataHolder.Incomplete
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.KeyValueStore
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.FileNameSerialiser
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.FileNameSerialiser.Companion.SEPARATOR
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.FileNameSerialiser.Companion.isValidFormat
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import java.io.*

class FileStore private constructor(
        private val logger: Logger,
        private val fileFactory: (File, String) -> File,
        private val fileInputStreamFactory: (File) -> InputStream,
        private val fileOutputStreamFactory: (File) -> OutputStream,
        private val fileReader: (InputStream) -> ByteArray,
        private val fileNameSerialiser: FileNameSerialiser,
        private val cacheDirectory: File
) : KeyValueStore<String, String, Incomplete> {

    init {
        cacheDirectory.mkdirs()
    }

    /**
     * Returns an existing entry key matching the given partial key
     *
     * @param partialKey the partial key used to retrieve the full key
     * @return the matching full key if present
     */
    override fun findPartialKey(partialKey: String) =
            cacheDirectory.list()
                    .firstOrNull { it.startsWith(partialKey + SEPARATOR) }

    /**
     * Returns an entry for the given key, if present
     *
     * @param key the entry's key
     * @return the matching entry if present
     */
    override fun get(key: String) =
            fileNameSerialiser.deserialise(key).copy(
                    data = fileInputStreamFactory(fileFactory(cacheDirectory, key)).useAndLogError(
                            fileReader::invoke,
                            logger
                    )
            )

    /**
     * Saves an entry with a given key
     *
     * @param key the key to save the entry under
     * @param value the value to associate with the key
     */
    override fun save(key: String, value: Incomplete) {
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
     * @return a map of the existing entries
     */
    override fun values() =
            cacheDirectory.list()!!
                    .filter(::isValidFormat)
                    .associateWith(::get)

    /**
     * Deletes the entry matching the given key
     *
     * @param key the entry's key
     */
    override fun delete(key: String) {
        fileFactory(cacheDirectory, key).delete()
    }

    /**
     * Renames an entry
     *
     * @param oldKey the old entry key
     * @param newKey  the new entry key
     */
    override fun rename(oldKey: String,
                        newKey: String) {
        fileFactory(cacheDirectory, oldKey)
                .renameTo(fileFactory(cacheDirectory, newKey))
    }

    class Factory<E> internal constructor(private val logger: Logger,
                                          private val dejaVuConfiguration: DejaVuConfiguration<E>,
                                          private val fileNameSerialiser: FileNameSerialiser)
            where E : Throwable,
                  E : NetworkErrorPredicate {

        fun create(cacheDirectory: File = dejaVuConfiguration.context.cacheDir) =
                FileStore(
                        logger,
                        ::File,
                        { BufferedInputStream(FileInputStream(it)) },
                        { BufferedOutputStream(FileOutputStream(it)) },
                        { it.readBytes() },
                        fileNameSerialiser,
                        cacheDirectory
                )
    }
}
