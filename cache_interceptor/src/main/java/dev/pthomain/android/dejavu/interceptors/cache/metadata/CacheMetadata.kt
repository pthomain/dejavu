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

package dev.pthomain.android.dejavu.interceptors.cache.metadata

import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate

/**
 * Contains cache metadata for the given call. This metadata is used on the ResponseWrapper and is added
 * to the target response if it implements CacheMetadata.Holder.
 *
 * This object is constructed via the ErrorFactory.
 * @see dev.pthomain.android.dejavu.interceptors.error.error.ErrorFactory
 *
 * @param cacheToken the cache token, containing information about the cache state of this response
 * @param exception any exception caught by the generic error handling or resulting of an exception during the caching process
 * @param callDuration how long the call took to execute at different stages of the caching process
 */

//TODO make this internal and use CacheResult externally, this is to avoid exposing internals of response/error classes and to stop merging response and error
data class CacheMetadata<E>(val cacheToken: CacheToken,
                            val exceptionClass: Class<E>,
                            val exception: E? = null,
                            val callDuration: CallDuration = CallDuration(0, 0, 0))
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * Interface to be set on any response requiring cache metadata to be provided. This metadata
     * contains information about the response's status, dates and serialisation methods.
     */
    interface Holder<E>
            where E : Exception,
                  E : NetworkErrorPredicate {
        var metadata: CacheMetadata<E>
    }

    override fun toString(): String {
        return "CacheMetadata(cacheToken=$cacheToken, exception=$exception, callDuration=$callDuration)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CacheMetadata<*>

        if (cacheToken != other.cacheToken) return false
        if (exceptionClass != other.exceptionClass) return false
        if (exception != other.exception) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cacheToken.hashCode()
        result = 31 * result + (exception?.hashCode() ?: 0)
        result = 31 * result + exceptionClass.hashCode()
        return result
    }

}