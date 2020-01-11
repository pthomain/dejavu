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

package dev.pthomain.android.dejavu.interceptors.error.error

import dev.pthomain.android.dejavu.interceptors.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CallDuration
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper

internal fun <E> ErrorFactory<E>.newWrapper(
        responseClass: Class<*>,
        response: Any?,
        metadata: CacheMetadata<E>
) where E : Exception,
        E : NetworkErrorPredicate =
        ResponseWrapper(
                responseClass,
                response,
                metadata
        )

internal fun <E> ErrorFactory<E>.newMetadata(
        cacheToken: CacheToken,
        exception: E? = null,
        callDuration: CallDuration = CallDuration(0, 0, 0)
) where E : Exception,
        E : NetworkErrorPredicate =
        CacheMetadata(
                cacheToken,
                exceptionClass,
                exception,
                callDuration
        )