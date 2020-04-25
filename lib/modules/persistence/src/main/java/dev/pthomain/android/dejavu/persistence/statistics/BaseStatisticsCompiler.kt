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

package dev.pthomain.android.dejavu.persistence.statistics

import dev.pthomain.android.dejavu.shared.token.CacheStatus.FRESH
import io.reactivex.Single
import java.util.*

/**
 * Skeletal implementation implementing StatisticsCompiler
 */
abstract class BaseStatisticsCompiler<T, I : Iterable<T>> : StatisticsCompiler {

    /**
     * @return a Single emitting cache statistics
     */
    final override fun getStatistics() =
            Single.fromCallable(::compileStatistics)

    /**
     * Returns an Iterable of the local metadata entries of a type that can be converted to CacheEntry.
     *
     * @see convert
     * @return the local entries containing the required metadata
     */
    abstract fun loadEntries(): I

    /**
     * Converts the given iterated local entry as returned by the loadEntries() method and
     * converts it to a CacheEntry.
     *
     * @param entry the local iterated entry as returned by the Iterable returned by loadEntries()
     * @see loadEntries
     * @return the converted CacheEntry
     */
    abstract fun convert(entry: T): CacheEntry

    /**
     * Compiles the statistics.
     *
     * @return the compiled statistics.
     */
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
            val fresh = values.count { it.status == FRESH }
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

        return CacheStatistics(entrySummaries)
    }

}