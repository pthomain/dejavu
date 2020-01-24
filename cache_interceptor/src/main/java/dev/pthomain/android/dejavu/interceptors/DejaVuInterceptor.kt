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

import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.interceptors.cache.CacheInterceptor
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CacheInstruction
import dev.pthomain.android.dejavu.interceptors.cache.instruction.PlainRequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.instruction.ValidRequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Cache
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.error.ErrorInterceptor
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.network.NetworkInterceptor
import dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.SingleTransformer
import java.util.*

/**
 * Wraps and composes with the interceptors dealing with error handling, cache and response decoration.
 *
 * @param isWrapped whether or not the response should be wrapped in a CacheResult
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
class DejaVuInterceptor<E> internal constructor(private val isWrapped: Boolean,
                                                private val operation: Operation,
                                                private val requestMetadata: PlainRequestMetadata,
                                                private val configuration: DejaVuConfiguration<E>,
                                                private val hasher: Hasher,
                                                private val dateFactory: (Long?) -> Date,
                                                private val hashingErrorObservableFactory: () -> Observable<Any>,
                                                private val errorInterceptorFactory: ErrorInterceptor.Factory<E>,
                                                private val networkInterceptorFactory: NetworkInterceptor.Factory<E>,
                                                private val cacheInterceptorFactory: CacheInterceptor.Factory<E>,
                                                private val responseInterceptorFactory: ResponseInterceptor.Factory<E>)
    : ObservableTransformer<Any, Any>,
        SingleTransformer<Any, Any>
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * Composes Observables with the wrapped interceptors
     *
     * @param upstream the call to intercept
     * @return the call intercepted with the inner interceptors
     */
    override fun apply(upstream: Observable<Any>) =
            composeInternal(upstream)

    /**
     * Composes Single with the wrapped interceptors
     *
     * @param upstream the call to intercept
     * @return the call intercepted with the inner interceptors
     */
    override fun apply(upstream: Single<Any>) =
            composeInternal(upstream.toObservable())
                    .firstOrError()!!

    /**
     * Deals with the internal composition.
     *
     * @param upstream the call to intercept
     * @param rxType the RxJava return type
     * @return the call intercepted with the inner interceptors
     */
    private fun composeInternal(upstream: Observable<Any>): Observable<Any> {
        val hashedRequestMetadata = hasher.hash(requestMetadata)

        return if (hashedRequestMetadata is ValidRequestMetadata) {

            val instruction = CacheInstruction(
                    operation,
                    hashedRequestMetadata
            )

            val instructionToken = CacheToken(instruction)

            val start = dateFactory(null).time
            val errorInterceptor = errorInterceptorFactory.create(instructionToken)

            upstream
                    .compose(networkInterceptorFactory.create(errorInterceptor, operation as? Cache, start))
                    .compose(cacheInterceptorFactory.create(errorInterceptor, instructionToken, start))
                    .compose(responseInterceptorFactory.create(instructionToken, isWrapped, start))

        } else hashingErrorObservableFactory().also {
            configuration.logger.e(this, "The request metadata could not be hashed, this request won't be cached: $requestMetadata")
        }
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
                                          private val errorInterceptorFactory: ErrorInterceptor.Factory<E>,
                                          private val networkInterceptorFactory: NetworkInterceptor.Factory<E>,
                                          private val cacheInterceptorFactory: CacheInterceptor.Factory<E>,
                                          private val responseInterceptorFactory: ResponseInterceptor.Factory<E>,
                                          private val configuration: DejaVuConfiguration<E>)
            where E : Exception,
                  E : NetworkErrorPredicate {

        /**
         * Provides an instance of DejaVuInterceptor
         *
         * @param isWrapped whether or not the response should be wrapped in a CacheResult
         * @param operation the cache operation for the intercepted call
         * @param requestMetadata the associated request metadata
         */
        fun create(isWrapped: Boolean,
                   operation: Operation,
                   requestMetadata: PlainRequestMetadata) =
                DejaVuInterceptor(
                        isWrapped,
                        operation,
                        requestMetadata,
                        configuration,
                        hasher,
                        dateFactory,
                        { Observable.error(IllegalStateException("The request could not be hashed")) },
                        errorInterceptorFactory,
                        networkInterceptorFactory,
                        cacheInterceptorFactory,
                        responseInterceptorFactory
                )
    }

}
