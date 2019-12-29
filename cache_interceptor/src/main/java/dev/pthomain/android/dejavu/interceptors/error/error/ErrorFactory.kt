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

import dev.pthomain.android.dejavu.interceptors.cache.instruction.DejaVuCall
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CacheMetadata.Duration
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import io.reactivex.Observable

/**
 * Converts a given Throwable into a new type extending from Exception and NetworkErrorPredicate
 */
interface ErrorFactory<E> : (Throwable) -> E
        where E : Exception,
              E : NetworkErrorPredicate {
    val exceptionClass: Class<E>

    fun newWrapper(responseClass: Class<*>,
                   response: Any?,
                   metadata: CacheMetadata<E>) =
            ResponseWrapper(
                    responseClass,
                    response,
                    metadata
            )

    fun newMetadata(cacheToken: CacheToken,
                    exception: E? = null,
                    callDuration: Duration = Duration(0, 0, 0)) =
            CacheMetadata(
                    cacheToken,
                    exceptionClass,
                    exception,
                    callDuration
            )

    fun <R : Any> newCacheOperation(
            observable: Observable<ResponseWrapper<E>>,
            responseClass: Class<R>
    ): DejaVuCall<R> =
            DejaVuCall.create(
                    observable.map {
                        ResponseWrapper(
                                responseClass,
                                it,
                                it.metadata
                        )
                    },
                    exceptionClass
            )
}
