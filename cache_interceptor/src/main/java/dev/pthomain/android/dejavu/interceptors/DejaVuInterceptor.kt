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
import dev.pthomain.android.dejavu.configuration.instruction.Operation
import dev.pthomain.android.dejavu.configuration.instruction.Operation.DoNotCache
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Expiring
import dev.pthomain.android.dejavu.interceptors.cache.CacheInterceptor
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.INSTRUCTION
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.error.ErrorInterceptor
import dev.pthomain.android.dejavu.interceptors.network.NetworkInterceptor
import dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.*
import io.reactivex.*
import io.reactivex.Observable
import java.util.*

/**
 * Wraps and composes with the interceptors dealing with error handling, cache and response decoration.
 *
 * @param operation the cache operation for the intercepted call
 * @param requestMetadata the associated request metadata
 * @param configuration the global cache configuration
 * @param hasher the class handling the request hashing for unicity
 * @param dateFactory the factory transforming timestamps to dates
 * @param hashingErrorObservableFactory the factory used to create a hashing error observable
 * @param networkInterceptorFactory the factory providing NetworkInterceptors dealing with network handling
 * @param errorInterceptorFactory the factory providing ErrorInterceptors dealing with network error handling
 * @param cacheInterceptorFactory the factory providing CacheInterceptors dealing with the cache
 * @param responseInterceptorFactory the factory providing ResponseInterceptors dealing with response decoration
 *
 * @see dev.pthomain.android.dejavu.interceptors.network.NetworkInterceptor
 * @see dev.pthomain.android.dejavu.interceptors.error.ErrorInterceptor
 * @see dev.pthomain.android.dejavu.interceptors.cache.CacheInterceptor
 * @see dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
 */
class DejaVuInterceptor<E> internal constructor(private val operation: Operation,
                                                private val requestMetadata: RequestMetadata.Plain,
                                                private val configuration: DejaVuConfiguration<E>,
                                                private val hasher: Hasher,
                                                private val dateFactory: (Long?) -> Date,
                                                private val hashingErrorObservableFactory: () -> Observable<Any>,
                                                private val errorInterceptorFactory: (CacheToken) -> ErrorInterceptor<E>,
                                                private val networkInterceptorFactory: (ErrorInterceptor<E>, CacheToken, Long) -> NetworkInterceptor<E>,
                                                private val cacheInterceptorFactory: (ErrorInterceptor<E>, CacheToken, Long) -> CacheInterceptor<E>,
                                                private val responseInterceptorFactory: (CacheToken, RxType, Long) -> ResponseInterceptor<E>)
    : ObservableTransformer<Any, Any>,
        SingleTransformer<Any, Any>,
        CompletableTransformer
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
            composeInternal(upstream.toObservable(), OPERATION)
                    .ignoreElements()!!

    /**
     * Deals with the internal composition.
     *
     * @param upstream the call to intercept
     * @param rxType the RxJava return type
     * @return the call intercepted with the inner interceptors
     */
    private fun composeInternal(upstream: Observable<Any>,
                                rxType: RxType): Observable<Any> {
        val hashedRequestMetadata: RequestMetadata.Hashed = hasher.hash(requestMetadata)

        val instruction = CacheInstruction(
                hashedRequestMetadata,
                ifElse(configuration.isCacheEnabled, operation, DoNotCache)
        )

        val instructionToken = CacheToken(
                instruction,
                INSTRUCTION,
                (instruction.operation as? Expiring)?.compress ?: configuration.compress,
                (instruction.operation as? Expiring)?.encrypt ?: configuration.encrypt
        )

        //TODO for CLEAR and INVALIDATE, use the type defined in the operation

        val start = dateFactory(null).time

        val observable = if (hashedRequestMetadata !is RequestMetadata.Hashed.Invalid) upstream
        else hashingErrorObservableFactory()

        val errorInterceptor = errorInterceptorFactory(instructionToken)

        return observable
                .compose(networkInterceptorFactory(errorInterceptor, instructionToken, start))
                .compose(cacheInterceptorFactory(errorInterceptor, instructionToken, start))
                .compose(responseInterceptorFactory(instructionToken, rxType, start))
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
     * @see dev.pthomain.android.dejavu.interceptors.error.ErrorInterceptor
     * @see dev.pthomain.android.dejavu.interceptors.network.NetworkInterceptor
     * @see dev.pthomain.android.dejavu.interceptors.cache.CacheInterceptor
     * @see dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
     */
    class Factory<E> internal constructor(private val hasher: Hasher,
                                          private val dateFactory: (Long?) -> Date,
                                          private val errorInterceptorFactory: (CacheToken) -> ErrorInterceptor<E>,
                                          private val networkInterceptorFactory: (ErrorInterceptor<E>, CacheToken, Long) -> NetworkInterceptor<E>,
                                          private val cacheInterceptorFactory: (ErrorInterceptor<E>, CacheToken, Long) -> CacheInterceptor<E>,
                                          private val responseInterceptorFactory: (CacheToken, RxType, Long) -> ResponseInterceptor<E>,
                                          private val configuration: DejaVuConfiguration<E>)
            where E : Exception,
                  E : NetworkErrorPredicate {

        /**
         * Provides an instance of DejaVuInterceptor
         *
         * @param operation the cache operation for the intercepted call
         * @param requestMetadata the associated request metadata
         */
        fun create(operation: Operation,
                   requestMetadata: RequestMetadata.Plain) =
                DejaVuInterceptor(
                        operation,
                        requestMetadata,
                        configuration,
                        hasher,
                        dateFactory,
                        { Observable.error<Any>(IllegalStateException("The request could not be hashed")) },
                        errorInterceptorFactory,
                        networkInterceptorFactory,
                        cacheInterceptorFactory,
                        responseInterceptorFactory
                )
    }

}
