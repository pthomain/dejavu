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

package dev.pthomain.android.dejavu.interceptors.cache.persistence.statistics

import android.annotation.SuppressLint
import com.jakewharton.fliptables.FlipTable
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Holds cache statistics
 *
 * @param configuration the cache configuration
 * @param entries compiled list of CacheEntrySummary
 */
data class CacheStatistics(
        val configuration: DejaVuConfiguration<*>,
        val entries: List<CacheEntrySummary>
) {
    override fun toString(): String {
        val formattedEntries = when {
            entries.isEmpty() -> FlipTable.of(
                    getCacheEntrySummaryColumnNames(true),
                    arrayOf(arrayOf(
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-"
                    ))
            )

            entries.size == 1 -> entries.first().toString()

            else -> FlipTable.of(
                    getCacheEntrySummaryColumnNames(true),
                    entries.map {
                        it.format(true)
                    }.toTypedArray()
            )
        }

        return "\n" + formattedEntries
    }

    /**
     * Use this method with a logger to prevent the table from being truncated by the max
     * logcat line limit. It outputs each line as a new log.
     *
     * @param logger a Logger instance to output the table
     * @param caller the caller or tag to use for the log
     */
    fun log(logger: Logger,
            caller: Any? = null) {
        toString().split("\n").forEach {
            logger.d(caller ?: this, it)
        }
    }
}

/**
 * Class holding statistics for a single type of response class
 */
data class CacheEntrySummary(
        val responseClass: Class<*>,
        val fresh: Int,
        val stale: Int,
        val oldestEntry: Date,
        val latestEntry: Date,
        val entries: List<CacheEntry>
) {
    override fun toString(): String {
        return FlipTable.of(
                getCacheEntrySummaryColumnNames(true),
                arrayOf(format(true))
        )
    }

    internal fun format(showClass: Boolean = false): Array<String> {
        val formattedEntries = FlipTable.of(
                getCacheEntryColumnNames(false),
                entries.map {
                    it.format(false)
                }.toTypedArray()
        )

        return arrayOf(
                ifElse(showClass, responseClass.format(), null),
                fresh.toString(),
                stale.toString(),
                dateFormat.format(oldestEntry),
                dateFormat.format(latestEntry),
                formattedEntries
        ).filterNotNull().toTypedArray()
    }

    /**
     * Use this method with a logger to prevent the table from being truncated by the max
     * logcat line limit. It outputs each line as a new log.
     *
     * @param logger a Logger instance to output the table
     * @param caller the caller or tag to use for the log
     */
    fun log(logger: Logger,
            caller: Any? = null) {
        toString().split("\n").forEach {
            logger.d(caller ?: this, it)
        }
    }
}

private fun getCacheEntrySummaryColumnNames(showClass: Boolean = false) =
        arrayOf(
                ifElse(showClass, "Response class", null),
                "Fresh",
                "Stale",
                "Oldest Entry",
                "Latest Entry",
                "Entries"
        ).filterNotNull().toTypedArray()

/**
 * Class holding statistics about a single cache entry (unique by URL and params)
 */
data class CacheEntry(
        val responseClass: Class<*>,
        val status: CacheStatus,
        val encrypted: Boolean,
        val compressed: Boolean,
        val cacheDate: Date,
        val expiryDate: Date
) {
    override fun toString() =
            FlipTable.of(
                    getCacheEntryColumnNames(true),
                    arrayOf(format(true))
            )

    internal fun format(showClass: Boolean = false) =
            arrayOf(
                    ifElse(showClass, responseClass.format(), null),
                    status.name,
                    ifElse(encrypted, "TRUE", "FALSE"),
                    ifElse(compressed, "TRUE", "FALSE"),
                    dateFormat.format(cacheDate),
                    dateFormat.format(expiryDate)
            ).filterNotNull().toTypedArray()

    /**
     * Use this method with a logger to prevent the table from being truncated by the max
     * logcat line limit. It outputs each line as a new log.
     *
     * @param logger a Logger instance to output the table
     * @param caller the caller or tag to use for the log
     */
    fun log(logger: Logger,
            caller: Any? = null) {
        toString().split("\n").forEach {
            logger.d(caller ?: this, it)
        }
    }
}

private fun getCacheEntryColumnNames(showClass: Boolean = false) =
        arrayOf(
                ifElse(showClass, "Response class", null),
                "Status",
                "Encrypted",
                "Compressed",
                "Cache Date",
                "Expiry Date"
        ).filterNotNull().toTypedArray()

@SuppressLint("SimpleDateFormat")
private val dateFormat = SimpleDateFormat("dd MMM yy 'at' HH:mm:ss.SS")

private fun Class<*>.format() = toString().replace("class ", "")