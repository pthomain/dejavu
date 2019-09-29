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

package dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.statistics

import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.database.CacheEntry
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.database.CacheEntrySummary
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.database.CacheStatistics
import dev.pthomain.android.dejavu.interceptors.internal.cache.token.CacheStatus
import io.reactivex.Single
import java.util.*

abstract class BaseStatisticsCompiler<T, I : Iterable<T>>(
        private val configuration: CacheConfiguration<*>
) : StatisticsCompiler {

    final override fun getStatistics() =
            Single.fromCallable(::compileStatistics)

    abstract fun loadEntries(): I
    abstract fun convert(entry: T): CacheEntry

    private fun compileStatistics(): CacheStatistics {
        val entries = mutableMapOf<Class<*>, MutableList<CacheEntry>>()

        loadEntries().forEach {
            val entry = convert(it)
            if (entries[entry.responseClass] == null) {
                entries[entry.responseClass] = mutableListOf()
            }
            entries[entry.responseClass]!!.add(entry)
        }

        val entrySummaries = entries.map {
            val values = it.value
            val fresh = values.count { it.status == CacheStatus.FRESH }
            val stale = values.size - fresh
            var oldest: Date? = null
            var latest: Date? = null

            values.forEach {
                with(it.cacheDate) {
                    when {
                        oldest == null && latest == null -> {
                            oldest = this; latest = this
                        }
                        before(oldest) -> oldest = this
                        after(latest) -> latest = this
                    }
                }
            }

            CacheEntrySummary(
                    values.first().responseClass,
                    fresh,
                    stale,
                    oldest!!,
                    latest!!,
                    values
            )
        }

        return CacheStatistics(
                configuration,
                entrySummaries
        )
    }

}