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

package dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.statistics.file

import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.PersistenceManager.Companion.getCacheStatus
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.database.CacheEntry
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.file.FileNameSerialiser
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.file.FileNameSerialiser.Companion.isValidFormat
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.statistics.BaseStatisticsCompiler
import java.io.File
import java.io.InputStream
import java.util.*

internal class FileStatisticsCompiler(
        private val configuration: CacheConfiguration<*>,
        private val fileFactory: (File, String) -> File,
        private val fileInputStreamFactory: (File) -> InputStream,
        private val dateFactory: (Long?) -> Date,
        private val fileNameSerialiser: FileNameSerialiser
) : BaseStatisticsCompiler<String, List<String>>(configuration) {

    override fun loadEntries() =
            configuration.cacheDirectory!!.list()
                    .filter { isValidFormat(it) }

    override fun convert(entry: String): CacheEntry {
        val dataHolder = fileNameSerialiser.deserialise(entry)!!

        val file = fileFactory(configuration.cacheDirectory!!, entry)
        val responseClassName = fileInputStreamFactory(file).reader().buffered().readLine()

        val status = getCacheStatus(
                dateFactory(dataHolder.expiryDate),
                dateFactory
        )

        return CacheEntry(
                Class.forName(responseClassName),
                status,
                dataHolder.isEncrypted,
                dataHolder.isCompressed,
                dateFactory(dataHolder.cacheDate),
                dateFactory(dataHolder.expiryDate)
        )
    }

}