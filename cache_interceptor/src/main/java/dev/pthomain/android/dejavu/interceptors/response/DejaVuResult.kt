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

import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CallDuration
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate

/**
 * Type to use as a return for calls returning response wrappers as defined below.
 * This is used as a marker for the call to emit one of the types extending this
 * sealed class and to provide the type of the response R.
 */
sealed class DejaVuResult<R : Any>

//The result has a response
data class Response<R : Any, O : Remote> internal constructor(
        val response: R,
        override var cacheToken: ResponseToken<O>,
        override var callDuration: CallDuration
) : DejaVuResult<R>(),
        HasCacheMetadata<O, ResponseToken<O>> by CacheMetadataHolder(cacheToken, callDuration)

//The result is empty due to filtering or exceptions
data class Empty<O : Remote, E> internal constructor(
        val exception: E,
        override var cacheToken: RequestToken<O>,
        override var callDuration: CallDuration
) : DejaVuResult<Any>(),
        HasCacheMetadata<O, RequestToken<O>> by CacheMetadataHolder(cacheToken, callDuration)
        where E : Exception, E : NetworkErrorPredicate

//The result has a response
data class Result<O : Local> internal constructor(
        override var cacheToken: RequestToken<O>,
        override var callDuration: CallDuration
) : DejaVuResult<Any>(),
        HasCacheMetadata<O, RequestToken<O>> by CacheMetadataHolder(cacheToken, callDuration)

internal data class CacheMetadataHolder<O : Operation, T : RequestToken<O>>(
        override var cacheToken: T,
        override var callDuration: CallDuration
) : HasCacheMetadata<O, T>

interface HasCacheMetadata<O : Operation, T : RequestToken<O>> : HasCacheToken<O, T>, HasCallDuration

interface HasCacheToken<O : Operation, T : RequestToken<O>> {
    var cacheToken: T
}

interface HasCallDuration {
    var callDuration: CallDuration
}