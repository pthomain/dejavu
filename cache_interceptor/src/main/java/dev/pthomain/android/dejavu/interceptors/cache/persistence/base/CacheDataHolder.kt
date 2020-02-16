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

package dev.pthomain.android.dejavu.interceptors.cache.persistence.base

import dev.pthomain.android.dejavu.interceptors.cache.instruction.HashedRequestMetadata

/**
 * Holds cache data for the purpose of persistence or filtering of the cached responses.
 *
 * @param cacheDate the optional date at which the response was cached
 * @param expiryDate the optional date at which the response will expire
 * @param data the serialised response payload
 * @param responseClassHash the hash of the response class
 * @param requestHash the hash of the request's URL, parameters and body
 * @param isCompressed whether or not the response was cached compressed
 * @param isEncrypted whether or not the response was cached encrypted
 */
sealed class CacheDataHolder(
        open val cacheDate: Long,
        open val expiryDate: Long,
        open val data: ByteArray,
        open val responseClassHash: String,
        open val requestHash: String,
        open val isCompressed: Boolean,
        open val isEncrypted: Boolean
) {

    /**
     * Holds the incomplete cache data for the purpose of filtering the cached responses only.
     *
     * @param cacheDate the optional date at which the response was cached
     * @param expiryDate the optional date at which the response will expire
     * @param data the serialised response payload
     * @param responseClassHash the hash of the response class
     * @param requestHash the hash of the request's URL, parameters and body
     * @param isCompressed whether or not the response was cached compressed
     * @param isEncrypted whether or not the response was cached encrypted
     */
    data class Incomplete(
            override val cacheDate: Long,
            override val expiryDate: Long,
            override val data: ByteArray,
            override val responseClassHash: String,
            override val requestHash: String,
            override val isCompressed: Boolean,
            override val isEncrypted: Boolean
    ) : CacheDataHolder(
            cacheDate,
            expiryDate,
            data,
            responseClassHash,
            requestHash,
            isCompressed,
            isEncrypted
    ) {
        override fun equals(other: Any?) = super.equals(other)
        override fun hashCode() = super.hashCode()
    }

    /**
     * Holds complete cache data for the purpose of persistence.
     *
     * @param requestMetadata the hashed request metadata containing the keys required for unicity
     * @param cacheDate the optional date at which the response was cached
     * @param expiryDate the optional date at which the response will expire
     * @param data the serialised response payload
     * @param isCompressed whether or not the response was cached compressed
     * @param isEncrypted whether or not the response was cached encrypted
     */
    data class Complete(
            val requestMetadata: HashedRequestMetadata,
            override val cacheDate: Long,
            override val expiryDate: Long,
            override val data: ByteArray,
            override val isCompressed: Boolean,
            override val isEncrypted: Boolean
    ) : CacheDataHolder(
            cacheDate,
            expiryDate,
            data,
            requestMetadata.classHash,
            requestMetadata.requestHash,
            isCompressed,
            isEncrypted
    ) {

        val incomplete = Incomplete(
                cacheDate,
                expiryDate,
                data,
                responseClassHash,
                requestHash,
                isCompressed,
                isEncrypted
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Complete
            if (requestMetadata != other.requestMetadata) return false
            return super.equals(other)
        }

        override fun hashCode(): Int {
            val result = requestMetadata.hashCode()
            return 31 * result + super.hashCode()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CacheDataHolder

        if (cacheDate != other.cacheDate) return false
        if (expiryDate != other.expiryDate) return false
        if (!data.contentEquals(other.data)) return false
        if (responseClassHash != other.responseClassHash) return false
        if (isCompressed != other.isCompressed) return false
        if (isEncrypted != other.isEncrypted) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cacheDate.hashCode()
        result = 31 * result + expiryDate.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + responseClassHash.hashCode()
        result = 31 * result + isCompressed.hashCode()
        result = 31 * result + isEncrypted.hashCode()
        return result
    }

}
