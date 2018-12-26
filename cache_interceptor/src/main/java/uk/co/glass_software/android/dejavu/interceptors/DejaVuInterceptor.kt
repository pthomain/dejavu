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

package uk.co.glass_software.android.dejavu.interceptors

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import uk.co.glass_software.android.boilerplate.utils.rx.RxIgnore
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.DoNotCache
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.CacheInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken.Companion.fromInstruction
import uk.co.glass_software.android.dejavu.interceptors.internal.error.ErrorInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.response.ResponseInterceptor
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.*
import java.util.*

class DejaVuInterceptor<E> private constructor(instruction: CacheInstruction,
                                               url: String,
                                               uniqueParameters: String?,
                                               configuration: CacheConfiguration<E>,
                                               private val dateFactory: (Long?) -> Date,
                                               private val responseInterceptorFactory: (CacheToken, Boolean, Boolean, Long) -> ResponseInterceptor<E>,
                                               private val errorInterceptorFactory: (CacheToken, Long) -> ErrorInterceptor<E>,
                                               private val cacheInterceptorFactory: (CacheToken, Long) -> CacheInterceptor<E>)
    : DejaVuTransformer
        where E : Exception,
              E : NetworkErrorProvider {

    private val instructionToken = fromInstruction(
            if (configuration.isCacheEnabled) instruction else instruction.copy(operation = DoNotCache),
            (instruction.operation as? Expiring)?.compress ?: configuration.compress,
            (instruction.operation as? Expiring)?.encrypt ?: configuration.encrypt,
            url,
            uniqueParameters
    )

    override fun apply(upstream: Observable<Any>) =
            composeInternal(upstream, OBSERVABLE)

    override fun apply(upstream: Single<Any>) =
            composeInternal(upstream.toObservable(), SINGLE)
                    .firstOrError()!!

    override fun apply(upstream: Completable) =
            composeInternal(upstream.andThen(RxIgnore.observable().cast(Any::class.java)), COMPLETABLE)
                    .ignoreElements()!!
//                    .onErrorResumeNext { error: Throwable ->
//                        //TODO check this
//                        if (error is NoSuchElementException || error.cause is NoSuchElementException) Completable.complete()
//                        else Completable.error(error)
//}

    private fun composeInternal(upstream: Observable<Any>,
                                rxType: AnnotationProcessor.RxType) =
            dateFactory(null).time.let { start ->
                upstream.compose(errorInterceptorFactory(instructionToken, start))
                        .compose(cacheInterceptorFactory(instructionToken, start))
                        .compose(responseInterceptorFactory(instructionToken, rxType == SINGLE, rxType == COMPLETABLE, start))
            }!!

    class Factory<E> internal constructor(private val dateFactory: (Long?) -> Date,
                                          private val errorInterceptorFactory: (CacheToken, Long) -> ErrorInterceptor<E>,
                                          private val cacheInterceptorFactory: (CacheToken, Long) -> CacheInterceptor<E>,
                                          private val responseInterceptorFactory: (CacheToken, Boolean, Boolean, Long) -> ResponseInterceptor<E>,
                                          private val configuration: CacheConfiguration<E>)
            where E : Exception,
                  E : NetworkErrorProvider {

        fun create(instruction: CacheInstruction,
                   url: String,
                   uniqueParameters: String?) =
                DejaVuInterceptor(
                        instruction,
                        url,
                        uniqueParameters,
                        configuration,
                        dateFactory,
                        responseInterceptorFactory,
                        errorInterceptorFactory,
                        cacheInterceptorFactory
                ) as DejaVuTransformer
    }
}
