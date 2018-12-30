/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.dejavu.interceptors.internal.cache.token

import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus.*
import java.util.*

data class CacheToken internal constructor(val instruction: CacheInstruction,
                                           val status: CacheStatus,
                                           val isCompressed: Boolean,
                                           val isEncrypted: Boolean,
                                           val apiUrl: String = "",
                                           val uniqueParameters: String? = null,
                                           val fetchDate: Date? = null,
                                           val cacheDate: Date? = null,
                                           val expiryDate: Date? = null) {

    companion object {

        internal fun fromInstruction(instruction: CacheInstruction,
                                     isCompressed: Boolean,
                                     isEncrypted: Boolean,
                                     apiUrl: String,
                                     uniqueParameters: String?) = CacheToken(
                instruction,
                INSTRUCTION,
                isCompressed,
                isEncrypted,
                apiUrl,
                uniqueParameters
        )

        internal fun notCached(instructionToken: CacheToken,
                               fetchDate: Date) = instructionToken.copy(
                status = NOT_CACHED,
                fetchDate = fetchDate
        )

        internal fun caching(instructionToken: CacheToken,
                             isCompressed: Boolean,
                             isEncrypted: Boolean,
                             fetchDate: Date,
                             cacheDate: Date,
                             expiryDate: Date) = instructionToken.copy(
                status = FRESH,
                isCompressed = isCompressed,
                isEncrypted = isEncrypted,
                fetchDate = fetchDate,
                cacheDate = cacheDate,
                expiryDate = expiryDate
        )

        internal fun cached(instructionToken: CacheToken,
                            status: CacheStatus,
                            isCompressed: Boolean,
                            isEncrypted: Boolean,
                            cacheDate: Date,
                            expiryDate: Date) = instructionToken.copy(
                status = status,
                isCompressed = isCompressed,
                isEncrypted = isEncrypted,
                cacheDate = cacheDate,
                fetchDate = cacheDate,
                expiryDate = expiryDate
        )

    }
}