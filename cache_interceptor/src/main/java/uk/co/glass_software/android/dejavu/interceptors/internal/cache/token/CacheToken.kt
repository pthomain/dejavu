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
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.RequestMetadata
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus.*
import java.util.*

data class CacheToken internal constructor(val instruction: CacheInstruction,
                                           val status: CacheStatus,
                                           val isCompressed: Boolean,
                                           val isEncrypted: Boolean,
                                           val requestMetadata: RequestMetadata.Hashed,
                                           val fetchDate: Date? = null,
                                           val cacheDate: Date? = null,
                                           val expiryDate: Date? = null) {

    companion object {

        internal fun fromInstruction(instruction: CacheInstruction,
                                     isCompressed: Boolean,
                                     isEncrypted: Boolean,
                                     requestMetadata: RequestMetadata.Hashed) = CacheToken(
                instruction,
                INSTRUCTION,
                isCompressed,
                isEncrypted,
                requestMetadata
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CacheToken

        if (instruction != other.instruction) return false
        if (status != other.status) return false
        if (isCompressed != other.isCompressed) return false
        if (isEncrypted != other.isEncrypted) return false
        if (requestMetadata != other.requestMetadata) return false
        if (fetchDate != other.fetchDate) return false
        if (cacheDate != other.cacheDate) return false
        if (expiryDate != other.expiryDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = instruction.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + isCompressed.hashCode()
        result = 31 * result + isEncrypted.hashCode()
        result = 31 * result + requestMetadata.hashCode()
        result = 31 * result + (fetchDate?.hashCode() ?: 0)
        result = 31 * result + (cacheDate?.hashCode() ?: 0)
        result = 31 * result + (expiryDate?.hashCode() ?: 0)
        return result
    }

}