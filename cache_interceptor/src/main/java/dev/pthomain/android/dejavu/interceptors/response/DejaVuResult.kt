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


package dev.pthomain.android.dejavu.interceptors.response

import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CallDuration
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.ResponseToken
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate

/**
 * Type to use as a return for calls returning response wrappers as defined below.
 * This is used as a marker for the call to emit one of the types extending this
 * sealed class and to provide the type of the response R.
 */
@Suppress("LeakingThis", "UNCHECKED_CAST") //sealed classes are final and all implement the interface
sealed class DejaVuResult<R : Any> {
    internal val hasMetadata = this as HasMetadata<R, *, *>
}

//The result has a response
data class Response<R : Any, O : Remote> internal constructor(
        val response: R,
        override var cacheToken: ResponseToken<O, R>,
        override var callDuration: CallDuration
) : DejaVuResult<R>(),
        HasResponseMetadata<R, O> by MetadataHolder(cacheToken, callDuration)

//The result is empty due to filtering or exceptions
class Empty<R : Any, O : Remote, E> internal constructor(
        val exception: E,
        override var cacheToken: RequestToken<O, R>,
        override var callDuration: CallDuration
) : DejaVuResult<R>(),
        HasRequestMetadata<R, O> by MetadataHolder(cacheToken, callDuration)
        where E : Throwable,
              E : NetworkErrorPredicate

//The result has no response (Local operation)
data class Result<R : Any, O : Local> internal constructor(
        override var cacheToken: RequestToken<O, R>,
        override var callDuration: CallDuration
) : DejaVuResult<R>(),
        HasRequestMetadata<R, O> by MetadataHolder(cacheToken, callDuration)

