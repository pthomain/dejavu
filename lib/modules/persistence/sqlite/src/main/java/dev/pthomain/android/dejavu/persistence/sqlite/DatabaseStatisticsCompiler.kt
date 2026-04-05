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

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.metadata.token.getCacheStatus
import dev.pthomain.android.dejavu.di.DateFactory
import dev.pthomain.android.dejavu.persistence.sqlite.dao.CacheDao
import dev.pthomain.android.dejavu.persistence.sqlite.entity.CacheEntry
import dev.pthomain.android.dejavu.persistence.statistics.BaseStatisticsCompiler
import dev.pthomain.android.dejavu.persistence.statistics.CacheEntry as StatsCacheEntry
import kotlinx.coroutines.runBlocking

/**
 * Provides a concrete StatisticsCompiler implementation for Room database entries.
 *
 * @param logger a Logger instance
 * @param dateFactory the factory converting timestamps to Dates
 * @param cacheDao the Room DAO for cache operations
 */
class DatabaseStatisticsCompiler internal constructor(
    private val logger: Logger,
    private val dateFactory: DateFactory,
    private val cacheDao: CacheDao
) : BaseStatisticsCompiler<CacheEntry, List<CacheEntry>>() {

    /**
     * Returns a list of the database entries.
     *
     * @return the list of cache entries
     */
    override fun loadEntries(): List<CacheEntry> =
        try {
            runBlocking { cacheDao.getAll() }
        } catch (e: Exception) {
            logger.e(this@DatabaseStatisticsCompiler, e, "Caught an exception loading entries")
            throw e
        }

    /**
     * Converts a Room CacheEntry to a statistics CacheEntry.
     *
     * @param entry the Room cache entry
     * @return the converted statistics entry
     */
    override fun convert(entry: CacheEntry): StatsCacheEntry {
        val cacheDate = dateFactory(entry.cacheDate)
        val expiryDate = dateFactory(entry.expiryDate)
        val status = dateFactory.getCacheStatus(expiryDate)

        return StatsCacheEntry(
            Class.forName(entry.classHash),
            status,
            cacheDate,
            expiryDate
        )
    }
}
