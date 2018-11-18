/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.cache_interceptor.retrofit

import io.reactivex.Observable
import retrofit2.Call
import retrofit2.CallAdapter
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ErrorInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.response.ResponseInterceptor
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.AnnotationProcessor
import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.AnnotationProcessor.RxType.*
import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.CacheException

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
                                                             errorInterceptorFactory: (CacheToken, Long) -> ErrorInterceptor<E>,
                                                             responseInterceptorFactory: (CacheToken, Boolean, Boolean, Long) -> ResponseInterceptor<E>,
                                                             cacheToken: CacheToken,
                                                             start: Long,
                                                             private val rxType: AnnotationProcessor.RxType,
                                                             exception: CacheException)
    : CallAdapter<Any, Any> by defaultAdapter
        where E : Exception,
              E : NetworkErrorProvider {

    private val errorInterceptor = errorInterceptorFactory(cacheToken, start)
    private val responseInterceptor = responseInterceptorFactory(
            cacheToken,
            false,
            false,
            start
    )

    private val errorObservable = Observable.error<Any>(exception)
            .compose(errorInterceptor::apply)
            .doOnNext {
                it.metadata = it.metadata.copy(
                        callDuration = CacheMetadata.Duration(
                                0,
                                0,
                                (System.currentTimeMillis() - start).toInt()
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
                COMPLETABLE -> errorObservable.ignoreElements()
            }!!

    class Factory<E>(private val errorInterceptorFactory: (CacheToken, Long) -> ErrorInterceptor<E>,
                     private val responseInterceptorFactory: (CacheToken, Boolean, Boolean, Long) -> ResponseInterceptor<E>)
            where E : Exception,
                  E : NetworkErrorProvider {

        fun create(defaultAdapter: CallAdapter<Any, Any>,
                   cacheToken: CacheToken,
                   start: Long,
                   rxType: AnnotationProcessor.RxType,
                   exception: CacheException) =
                ProcessingErrorAdapter(
                        defaultAdapter,
                        errorInterceptorFactory,
                        responseInterceptorFactory,
                        cacheToken,
                        start,
                        rxType,
                        exception
                )
    }
}