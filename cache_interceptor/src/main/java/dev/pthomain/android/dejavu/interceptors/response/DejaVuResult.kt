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

import dev.pthomain.android.dejavu.interceptors.cache.instruction.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CallDuration
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.EmptyRemoteToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.LocalToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.StatusToken
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseWrapperFactory.EmptyResponseException

//TODO JavaDoc
sealed class DejaVuResult<R : Any, T : StatusToken>(
        override val requestMetadata: RequestMetadata,
        override val cacheToken: T,
        override val callDuration: CallDuration
) : HasCacheMetadata<T>

//The result has a response
data class Response<R : Any> internal constructor(
        val response: R,
        override val requestMetadata: RequestMetadata,
        override val cacheToken: ResponseToken,
        override val callDuration: CallDuration
) : DejaVuResult<R, ResponseToken>(requestMetadata, cacheToken, callDuration)

//The result is empty due to filtering or exceptions
data class Empty<R : Any> internal constructor(
        val exception: EmptyResponseException,
        override val requestMetadata: RequestMetadata,
        override val cacheToken: EmptyRemoteToken,
        override val callDuration: CallDuration
) : DejaVuResult<R, EmptyRemoteToken>(requestMetadata, cacheToken, callDuration)

//The result has a response
data class Result<R : Any> internal constructor(
        override val requestMetadata: RequestMetadata,
        override val cacheToken: LocalToken,
        override val callDuration: CallDuration
) : DejaVuResult<R, LocalToken>(requestMetadata, cacheToken, callDuration)

interface HasCacheMetadata<T : StatusToken> : HasRequestMetadata, HasCacheToken<T>, HasCallDuration

interface HasRequestMetadata {
    val requestMetadata: RequestMetadata
}

interface HasCacheToken<T : StatusToken> {
    val cacheToken: T
}

interface HasCallDuration {
    val callDuration: CallDuration
}