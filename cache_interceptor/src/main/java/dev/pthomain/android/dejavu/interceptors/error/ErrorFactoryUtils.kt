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

package dev.pthomain.android.dejavu.interceptors.error

import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CallDuration
import dev.pthomain.android.dejavu.interceptors.cache.metadata.ResponseMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.RequestToken
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate

internal fun <O : Operation, T : RequestToken<O>, E> ErrorFactory<E>.newWrapper(
        response: Any?,
        metadata: ResponseMetadata<O, T, E>
) where E : Throwable,
        E : NetworkErrorPredicate =
        ResponseWrapper(
                response,
                metadata
        )

internal fun <O : Operation, T : RequestToken<O>, E> ErrorFactory<E>.newMetadata(
        cacheToken: T,
        exception: E? = null,
        callDuration: CallDuration = CallDuration(0, 0, 0)
) where E : Throwable,
        E : NetworkErrorPredicate =
        ResponseMetadata(
                cacheToken,
                exceptionClass,
                exception,
                callDuration
        )