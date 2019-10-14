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

package dev.pthomain.android.dejavu.interceptors

import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.configuration.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.DoNotCache
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Expiring
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.INSTRUCTION
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.error.ErrorInterceptor
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.retrofit.RetrofitCallAdapterFactory.Companion.INVALID_HASH
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.*
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import java.util.*

/**
 * Wraps and composes with the interceptors dealing with error handling, cache and response decoration.
 *
 * @param instruction the cache instruction for the intercepted call
 * @param requestMetadata the associated request metadata
 * @param configuration the global cache configuration
 * @param hasher the class handling the request hashing for unicity
 * @param dateFactory the factory transforming timestamps to dates
 * @param networkInterceptorFactory the factory providing NetworkInterceptors dealing with network handling
 * @param errorInterceptorFactory the factory providing ErrorInterceptors dealing with network error handling
 * @param cacheInterceptorFactory the factory providing CacheInterceptors dealing with the cache
 * @param responseInterceptorFactory the factory providing ResponseInterceptors dealing with response decoration
 *
 * @see dev.pthomain.android.dejavu.interceptors.internal.network.NetworkInterceptor
 * @see dev.pthomain.android.dejavu.interceptors.internal.error.ErrorInterceptor
 * @see dev.pthomain.android.dejavu.interceptors.internal.cache.CacheInterceptor
 * @see dev.pthomain.android.dejavu.interceptors.internal.response.ResponseInterceptor
 */
class DejaVuInterceptor<E> private constructor(private val instruction: CacheInstruction,
                                               private val requestMetadata: RequestMetadata.UnHashed,
                                               private val configuration: DejaVuConfiguration<E>,
                                               private val hasher: Hasher,
                                               private val dateFactory: (Long?) -> Date,
                                               private val errorInterceptorFactory: (CacheToken) -> ObservableTransformer<Any, ResponseWrapper<E>>,
                                               private val networkInterceptorFactory: (ErrorInterceptor<E>, CacheToken, Long) -> ObservableTransformer<Any, ResponseWrapper<E>>,
                                               private val cacheInterceptorFactory: (ErrorInterceptor<E>, CacheToken, Long) -> ObservableTransformer<ResponseWrapper<E>, ResponseWrapper<E>>,
                                               private val responseInterceptorFactory: (CacheToken, Boolean, Boolean, Long) -> ObservableTransformer<ResponseWrapper<E>, Any>)
    : DejaVuTransformer
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * Composes Observables with the wrapped interceptors
     *
     * @param upstream the call to intercept
     * @return the call intercepted with the inner interceptors
     */
    override fun apply(upstream: Observable<Any>) =
            composeInternal(upstream, OBSERVABLE)

    /**
     * Composes Single with the wrapped interceptors
     *
     * @param upstream the call to intercept
     * @return the call intercepted with the inner interceptors
     */
    override fun apply(upstream: Single<Any>) =
            composeInternal(upstream.toObservable(), SINGLE)
                    .firstOrError()!!

    /**
     * Composes Completables with the wrapped interceptors
     *
     * @param upstream the call to intercept
     * @return the call intercepted with the inner interceptors
     */
    override fun apply(upstream: Completable) =
            composeInternal(upstream.toObservable(), COMPLETABLE)
                    .ignoreElements()!!

    /**
     * Deals with the internal composition.
     *
     * @param upstream the call to intercept
     * @param rxType the RxJava return type
     * @return the call intercepted with the inner interceptors
     */
    private fun composeInternal(upstream: Observable<Any>,
                                rxType: AnnotationProcessor.RxType): Observable<Any> {
        val (hashedRequestMetadata, isHashed) = hasher.hash(requestMetadata).let {
            ifElse(it == null,
                    RequestMetadata.Hashed(
                            requestMetadata.responseClass,
                            requestMetadata.url,
                            requestMetadata.requestBody,
                            INVALID_HASH,
                            INVALID_HASH
                    ) to false,
                    it!! to true
            )
        }

        val instructionToken = CacheToken(
                if (configuration.isCacheEnabled) instruction else instruction.copy(operation = DoNotCache),
                INSTRUCTION,
                (instruction.operation as? Expiring)?.compress ?: configuration.compress,
                (instruction.operation as? Expiring)?.encrypt ?: configuration.encrypt,
                hashedRequestMetadata
        )

        val start = dateFactory(null).time

        val observable = if (isHashed) upstream
        else Observable.error<Any>(IllegalStateException("The request could not be hashed"))

        val errorInterceptor = errorInterceptorFactory(instructionToken) as ErrorInterceptor<E>

        return observable
                .compose(networkInterceptorFactory(errorInterceptor, instructionToken, start))
                .compose(cacheInterceptorFactory(errorInterceptor, instructionToken, start))
                .compose(responseInterceptorFactory(instructionToken, rxType == SINGLE, rxType == COMPLETABLE, start))
    }

    /**
     * Factory providing instances of DejaVuInterceptor
     *
     * @param hasher the class handling the request hashing for unicity
     * @param dateFactory the factory transforming timestamps to dates
     * @param networkInterceptorFactory the factory providing NetworkInterceptors dealing with network handling
     * @param errorInterceptorFactory the factory providing ErrorInterceptors dealing with error handling
     * @param cacheInterceptorFactory the factory providing CacheInterceptors dealing with the cache
     * @param responseInterceptorFactory the factory providing ResponseInterceptors dealing with response decoration
     * @param configuration the global cache configuration
     *
     * @see dev.pthomain.android.dejavu.interceptors.internal.error.ErrorInterceptor
     * @see dev.pthomain.android.dejavu.interceptors.internal.network.NetworkInterceptor
     * @see dev.pthomain.android.dejavu.interceptors.internal.cache.CacheInterceptor
     * @see dev.pthomain.android.dejavu.interceptors.internal.response.ResponseInterceptor
     */
    class Factory<E> internal constructor(private val hasher: Hasher,
                                          private val dateFactory: (Long?) -> Date,
                                          private val errorInterceptorFactory: (CacheToken) -> ObservableTransformer<Any, ResponseWrapper<E>>,
                                          private val networkInterceptorFactory: (ErrorInterceptor<E>, CacheToken, Long) -> ObservableTransformer<Any, ResponseWrapper<E>>,
                                          private val cacheInterceptorFactory: (ErrorInterceptor<E>, CacheToken, Long) -> ObservableTransformer<ResponseWrapper<E>, ResponseWrapper<E>>,
                                          private val responseInterceptorFactory: (CacheToken, Boolean, Boolean, Long) -> ObservableTransformer<ResponseWrapper<E>, Any>,
                                          private val configuration: DejaVuConfiguration<E>)
            where E : Exception,
                  E : NetworkErrorPredicate {

        /**
         * Provides an instance of DejaVuInterceptor
         *
         * @param instruction the cache instruction for the intercepted call
         * @param requestMetadata the associated request metadata
         */
        fun create(instruction: CacheInstruction,
                   requestMetadata: RequestMetadata.UnHashed) =
                DejaVuInterceptor(
                        instruction,
                        requestMetadata,
                        configuration,
                        hasher,
                        dateFactory,
                        errorInterceptorFactory,
                        networkInterceptorFactory,
                        cacheInterceptorFactory,
                        responseInterceptorFactory
                ) as DejaVuTransformer
    }

}
