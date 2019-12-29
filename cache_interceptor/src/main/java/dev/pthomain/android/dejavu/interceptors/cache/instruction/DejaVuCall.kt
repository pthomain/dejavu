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

/**
 * Observable wrapper capturing the target response type and wrapping the exception returned by the
 * error factory into a generic DejaVuCallException.
 *
 * This is useful primarily for operations that may not return a result (i.e. Clear or Invalidate)
 * and can be considered Completable operations but still require information about the target type
 * (which cannot be provided by a Completable).
 *
 * By extension, any call can use DejavuCall which will emit a ResponseWrapper, which will either
 * contain a response of the given target type or an exception in the metadata if no response is
 * available. For operations of type Clear or Invalidate, the exception for a successful execution
 * (returning no response) would be a DoneException.
 *
 * @see dev.pthomain.android.dejavu.interceptors.response.EmptyResponseWrapperFactory.DoneException
 */
class DejaVuCall<R> private constructor(
        private val observable: Observable<ResponseWrapper<DejaVuCallException>>,
        val exceptionClass: Class<*>
) : Observable<ResponseWrapper<DejaVuCallException>>() {

    override fun subscribeActual(observer: Observer<in ResponseWrapper<DejaVuCallException>>) {
        observable.subscribe(observer)
    }

    companion object {

        fun <R, E> create(
                observable: Observable<ResponseWrapper<E>>,
                exceptionClass: Class<E>
        ) where E : Exception, E : NetworkErrorPredicate = DejaVuCall<R>(
                observable.map(::mapToOperationException),
                exceptionClass
        )
    }
}

private fun <E> mapToOperationException(originalWrapper:ResponseWrapper<E>)
        : ResponseWrapper<DejaVuCallException>
        where E : Exception, E : NetworkErrorPredicate = with(originalWrapper) {
    ResponseWrapper(
            responseClass,
            response,
            with(metadata) {
                CacheMetadata(
                        cacheToken,
                        DejaVuCallException::class.java,
                        exception?.let { DejaVuCallException(it, it) },
                        callDuration
                )
            }
    )
}

class DejaVuCallException internal constructor(
        originalException: Exception,
        originalPredicate: NetworkErrorPredicate
) : Exception(originalException.message, originalException.cause),
        NetworkErrorPredicate by originalPredicate

