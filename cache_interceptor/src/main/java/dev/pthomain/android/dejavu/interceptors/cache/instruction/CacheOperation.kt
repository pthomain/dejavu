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

package dev.pthomain.android.dejavu.interceptors.cache.instruction

import dev.pthomain.android.dejavu.interceptors.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
import io.reactivex.Observable
import io.reactivex.Observer

sealed class CacheOperation<R>(
        private val observable: Observable<ResponseWrapper<CacheOperationException>>,
        val exceptionClass: Class<*>
) : Observable<ResponseWrapper<CacheOperationException>>() {
    override fun subscribeActual(observer: Observer<in ResponseWrapper<CacheOperationException>>) {
        observable.subscribe(observer)
    }

    class Resolved<R, E>(
            observable: Observable<ResponseWrapper<E>>,
            exceptionClass: Class<E>
    ) : CacheOperation<R>(
            observable.map(::mapToOperationException),
            exceptionClass
    ) where E : Exception, E : NetworkErrorPredicate

}

private fun <E> mapToOperationException(originalWrapper:ResponseWrapper<E>)
        : ResponseWrapper<CacheOperationException>
        where E : Exception, E : NetworkErrorPredicate = with(originalWrapper) {
    ResponseWrapper(
            responseClass,
            response,
            with(metadata) {
                CacheMetadata(
                        cacheToken,
                        CacheOperationException::class.java,
                        exception?.let { CacheOperationException(it, it) },
                        callDuration
                )
            }
    )
}

class CacheOperationException internal constructor(
        originalException: Exception,
        originalPredicate: NetworkErrorPredicate
) : Exception(originalException.message, originalException.cause),
        NetworkErrorPredicate by originalPredicate

