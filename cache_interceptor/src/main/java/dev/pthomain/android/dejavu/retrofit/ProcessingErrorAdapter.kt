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

package dev.pthomain.android.dejavu.retrofit

import dev.pthomain.android.dejavu.configuration.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.error.ErrorInterceptor
import dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.*
import dev.pthomain.android.dejavu.retrofit.annotations.CacheException
import io.reactivex.Completable
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.CallAdapter
import java.util.*

/**
 * This class adapts a call with a configuration error and emits this error via the expected RxJava type.
 *
 * @param defaultAdapter the default RxJava adapter
 * @param errorInterceptorFactory the error interceptor factory used to provide an error response to wrap the given exception
 * @param responseInterceptorFactory the response interceptor factory, used to decorate the response with metadata or emit the error via the default RxJava error mechanism according to the mergeOnNextOnError directive
 * @param cacheToken the instruction cache token
 * @param start the timestamp of the call's start
 * @param rxType the expected RxJava return type
 * @param exception the caught exception to be processed
 */
internal class ProcessingErrorAdapter<E> private constructor(defaultAdapter: CallAdapter<Any, Any>,
                                                             errorInterceptorFactory: (CacheToken) -> ErrorInterceptor<E>,
                                                             responseInterceptorFactory: (CacheToken, Boolean, Boolean, Long) -> ResponseInterceptor<E>,
                                                             private val dateFactory: (Long?) -> Date,
                                                             cacheToken: CacheToken,
                                                             start: Long,
                                                             private val rxType: AnnotationProcessor.RxType,
                                                             exception: CacheException)
    : CallAdapter<Any, Any> by defaultAdapter
        where E : Exception,
              E : NetworkErrorPredicate {

    private val errorInterceptor = errorInterceptorFactory(cacheToken)

    private val responseInterceptor = responseInterceptorFactory(
            cacheToken,
            false,
            false,
            start
    )

    private val errorObservable = errorInterceptor.apply(Observable.error(exception))
            .doOnNext {
                it.metadata = it.metadata.copy(
                        callDuration = CacheMetadata.Duration(
                                0,
                                0,
                                (dateFactory(null).time - start).toInt()
                        )
                )
            }
            .compose(responseInterceptor)

    /**
     * Adapts the call to a RxJava type
     */
    override fun adapt(call: Call<Any>) =
            when (rxType) {
                OBSERVABLE -> errorObservable
                SINGLE -> errorObservable.firstOrError()
                COMPLETABLE -> errorObservable.flatMapCompletable {
                    if (it is CacheMetadata.Holder<*> && it.metadata.exception != null) {
                        Completable.error(it.metadata.exception)
                    } else Completable.complete()
                }
            }!!

    class Factory<E>(private val errorInterceptorFactory: (CacheToken) -> ErrorInterceptor<E>,
                     private val responseInterceptorFactory: (CacheToken, Boolean, Boolean, Long) -> ResponseInterceptor<E>,
                     private val dateFactory: (Long?) -> Date)
            where E : Exception,
                  E : NetworkErrorPredicate {

        fun create(defaultAdapter: CallAdapter<Any, Any>,
                   cacheToken: CacheToken,
                   start: Long,
                   rxType: AnnotationProcessor.RxType,
                   exception: CacheException) =
                ProcessingErrorAdapter(
                        defaultAdapter,
                        errorInterceptorFactory,
                        responseInterceptorFactory,
                        dateFactory,
                        cacheToken,
                        start,
                        rxType,
                        exception
                ) as CallAdapter<*, *>
    }
}