/*
 *
 *  Copyright (C) 2017-2020 Pierre Thomain
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

import dev.pthomain.android.dejavu.DejaVu.Configuration
import dev.pthomain.android.dejavu.cache.Hasher
import dev.pthomain.android.dejavu.cache.metadata.response.*
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.INSTRUCTION
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.NETWORK
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.CacheInstruction
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.PlainRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.ValidRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
import dev.pthomain.android.dejavu.retrofit.OperationResolver
import dev.pthomain.android.dejavu.retrofit.glitchy.OperationReturnType
import dev.pthomain.android.glitchy.interceptor.Interceptor
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.interceptor.outcome.Outcome
import dev.pthomain.android.glitchy.interceptor.outcome.Outcome.Error
import dev.pthomain.android.glitchy.interceptor.outcome.Outcome.Success
import dev.pthomain.android.glitchy.retrofit.type.ParsedType
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Call
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
 * @param cacheInterceptorFactory the factory providing CacheInterceptors dealing with the cache
 * @param responseInterceptorFactory the factory providing ResponseInterceptors dealing with response decoration
 *
 * @see dev.pthomain.android.dejavu.interceptors.network.NetworkInterceptor
 * @see dev.pthomain.android.dejavu.interceptors.cache.CacheInterceptor
 * @see dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
 */
class DejaVuInterceptor<E, R : Any> internal constructor(
        private val isWrapped: Boolean,
        private val operation: Operation,
        private val requestMetadata: PlainRequestMetadata<R>,
        private val configuration: Configuration<E>,
        private val hasher: Hasher,
        private val dateFactory: (Long?) -> Date,
        private val hashingErrorObservableFactory: () -> Observable<Any>,
        private val networkInterceptorFactory: NetworkInterceptor.Factory<E>,
        private val cacheInterceptorFactory: CacheInterceptor.Factory<E>,
        private val responseInterceptorFactory: ResponseInterceptor.Factory<E>
) : Interceptor
        where E : Throwable,
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
     * Composes Observables with the wrapped interceptors and only emits the
     * final response (if intercepted).
     *
     * @param upstream the call to intercept
     * @return the call intercepted with the inner interceptors
     */
    override fun apply(upstream: Single<Any>) =
            upstream.toObservable()
                    .compose(this)
                    .filter { (it as? HasMetadata<*, *, *>)?.cacheToken?.status?.isFinal ?: true }
                    .firstOrError()

    /**
     * Deals with the internal composition.
     *
     * @param upstream the call to intercept
     * @return the call intercepted with the inner interceptors
     */
    private fun composeInternal(upstream: Observable<Any>): Observable<Any> {
        val requestDate = dateFactory(null)
        val hashedRequestMetadata = hasher.hash(requestMetadata)

        return if (hashedRequestMetadata is ValidRequestMetadata<R>) {
            val instruction = CacheInstruction(
                    operation,
                    hashedRequestMetadata
            )

            val instructionToken = RequestToken(
                    instruction,
                    INSTRUCTION,
                    requestDate
            )

            val cacheInterceptor = cacheInterceptorFactory.create(instructionToken)
            val responseInterceptor = responseInterceptorFactory.create<R>(isWrapped)

            @Suppress("UNCHECKED_CAST")
            if (operation is Remote) {
                instructionToken as RequestToken<out Remote, R>
                upstream.map { checkOutcome(it, instructionToken) }
                        .compose(networkInterceptorFactory.create(instructionToken))
                        .compose(cacheInterceptor)
                        .compose(responseInterceptor)
            } else {
                Observable.just(LocalOperationToken<R>())
                        .compose(cacheInterceptor)
                        .compose(responseInterceptor)
            }
        } else {
            configuration.logger.e(
                    this,
                    "The request metadata could not be hashed, this request won't be cached: $requestMetadata"
            )
            hashingErrorObservableFactory()
        }
    }

    private fun <O : Remote> checkOutcome(
            outcome: Any,
            instructionToken: RequestToken<O, R>
    ): DejaVuResult<R> {
        val callDuration = CallDuration(
                0,
                (dateFactory(null).time - instructionToken.requestDate.time).toInt(),
                0
        )

        @Suppress("UNCHECKED_CAST") //converted to Outcome by DejaVuReturnTypeParser
        return when (outcome as Outcome<R>) {
            is Success<R> -> Response(
                    (outcome as Success<R>).response,
                    ResponseToken(
                            instructionToken.instruction,
                            NETWORK,
                            instructionToken.requestDate
                    ),
                    callDuration
            )
            is Error<*> -> Empty(
                    (outcome as Error<E>).exception,
                    instructionToken,
                    callDuration
            )
        }
    }

    /**
     * Factory providing instances of DejaVuInterceptor
     *
     * @param hasher the class handling the request hashing for unicity
     * @param dateFactory the factory transforming timestamps to dates
     * @param networkInterceptorFactory the factory providing NetworkInterceptors dealing with network handling
     * @param cacheInterceptorFactory the factory providing CacheInterceptors dealing with the cache
     * @param responseInterceptorFactory the factory providing ResponseInterceptors dealing with response decoration
     * @param configuration the global cache configuration
     *
     * @see dev.pthomain.android.dejavu.interceptors.network.NetworkInterceptor
     * @see dev.pthomain.android.dejavu.interceptors.cache.CacheInterceptor
     * @see dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
     */
    class Factory<E> internal constructor(
            private val hasher: Hasher,
            private val dateFactory: (Long?) -> Date,
            private val networkInterceptorFactory: NetworkInterceptor.Factory<E>,
            private val cacheInterceptorFactory: CacheInterceptor.Factory<E>,
            private val responseInterceptorFactory: ResponseInterceptor.Factory<E>,
            private val operationResolverFactory: OperationResolver.Factory<E>,
            private val configuration: Configuration<E>
    ) where E : Throwable,
            E : NetworkErrorPredicate {

        internal val glitchyFactory = object : Interceptor.Factory<E> {
            override fun <M> create(
                    parsedType: ParsedType<M>,
                    call: Call<Any>
            ): Interceptor? =
                    with(parsedType.metadata) {
                        if (this is OperationReturnType) {
                            operationResolverFactory.create(
                                    methodDescription,
                                    dejaVuReturnType.responseClass,
                                    annotationOperation
                            ).getResolvedOperation(call)?.run {
                                create(
                                        dejaVuReturnType.isDejaVuResult,
                                        operation,
                                        requestMetadata as PlainRequestMetadata<out Any>
                                )
                            }
                        } else null
                    }
        }

        /**
         * Provides an instance of DejaVuInterceptor
         *
         * @param isWrapped whether or not the response should be wrapped in a CacheResult
         * @param operation the cache operation for the intercepted call
         * @param requestMetadata the associated request metadata
         */
        fun <R : Any> create(isWrapped: Boolean,
                             operation: Operation,
                             requestMetadata: PlainRequestMetadata<R>) =
                DejaVuInterceptor(
                        isWrapped,
                        operation,
                        requestMetadata,
                        configuration,
                        hasher,
                        dateFactory,
                        { Observable.error(IllegalStateException("The request could not be hashed")) },
                        networkInterceptorFactory,
                        cacheInterceptorFactory,
                        responseInterceptorFactory
                )
    }

}