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

package dev.pthomain.android.dejavu.interceptors.response

import dev.pthomain.android.dejavu.interceptors.cache.metadata.CallDuration
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseWrapperFactory.EmptyResponseException

//TODO JavaDoc
sealed class DejaVuResult<R : Any>(
        override val requestMetadata: RequestMetadata,
        override val cacheToken: CacheToken,
        override val callDuration: CallDuration
) : HasCacheMetadata {

    //The result has a response
    data class Response<R : Any> internal constructor(
            val response: R,
            override val requestMetadata: RequestMetadata,
            override val cacheToken: CacheToken,
            override val callDuration: CallDuration
    ) : DejaVuResult<R>(requestMetadata, cacheToken, callDuration)

    //The result is empty due to filtering or exceptions
    data class Empty<R : Any> internal constructor(
            val exception: EmptyResponseException,
            override val requestMetadata: RequestMetadata,
            override val cacheToken: CacheToken,
            override val callDuration: CallDuration
    ) : DejaVuResult<R>(requestMetadata, cacheToken, callDuration)

    //The result has a response
    data class Operation<R : Any> internal constructor(
            override val requestMetadata: RequestMetadata,
            override val cacheToken: CacheToken,
            override val callDuration: CallDuration
    ) : DejaVuResult<R>(requestMetadata, cacheToken, callDuration)

}

interface HasCacheMetadata : HasRequestMetadata, HasCacheToken, HasCallDuration

interface HasRequestMetadata {
    val requestMetadata: RequestMetadata
}

interface HasCacheToken {
    val cacheToken: CacheToken
}

interface HasCallDuration {
    val callDuration: CallDuration
}