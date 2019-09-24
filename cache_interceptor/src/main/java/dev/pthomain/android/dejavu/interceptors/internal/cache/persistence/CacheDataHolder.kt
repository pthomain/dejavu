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

package dev.pthomain.android.dejavu.interceptors.internal.cache.persistence

import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.RequestMetadata

data class CacheDataHolder(
        val requestMetadata: RequestMetadata.Hashed?,
        val cacheDate: Long,
        val expiryDate: Long,
        val data: ByteArray,
        val responseClassName: String,
        val isCompressed: Boolean,
        val isEncrypted: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CacheDataHolder

        if (requestMetadata != other.requestMetadata) return false
        if (cacheDate != other.cacheDate) return false
        if (expiryDate != other.expiryDate) return false
        if (!data.contentEquals(other.data)) return false
        if (responseClassName != other.responseClassName) return false
        if (isCompressed != other.isCompressed) return false
        if (isEncrypted != other.isEncrypted) return false

        return true
    }

    override fun hashCode(): Int {
        var result = requestMetadata.hashCode()
        result = 31 * result + cacheDate.hashCode()
        result = 31 * result + expiryDate.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + responseClassName.hashCode()
        result = 31 * result + isCompressed.hashCode()
        result = 31 * result + isEncrypted.hashCode()
        return result
    }
}