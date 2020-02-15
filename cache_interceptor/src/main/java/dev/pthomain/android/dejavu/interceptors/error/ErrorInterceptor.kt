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

package dev.pthomain.android.dejavu.interceptors.error

import android.content.Context
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.boilerplate.core.utils.rx.observable
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.NETWORK
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.InstructionToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseWrapperFactory
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.Function
import java.util.*

/**
 * Interceptor handling network exceptions, converting them using the chosen ErrorFactory and
 * returning a ResponseWrapper holding the response or exception.
 *
 * @see ErrorFactory
 * @param context the application context
 * @param errorFactory the factory converting throwables to custom exceptions
 * @param emptyResponseWrapperFactory factory providing empty response wrappers
 * @param logger a Logger instance
 * @param dateFactory a factory converting timestamps to Dates
 * @param instructionToken the original request's instruction token
 */
internal class ErrorInterceptor<O : Operation, T : RequestToken<O>, E> private constructor(
        private val context: Context,
        private val errorFactory: ErrorFactory<E>,
        private val emptyResponseWrapperFactory: EmptyResponseWrapperFactory<E>,
        private val logger: Logger,
        private val dateFactory: (Long?) -> Date,
        private val instructionToken: InstructionToken<O>
) : ObservableTransformer<Any, ResponseWrapper<O, T, E>>
        where E : Exception,
              E : NetworkErrorPredicate {

    private val responseClass = instructionToken.instruction.requestMetadata.responseClass

    /**
     * The composition method converting an upstream response Observable to an Observable emitting
     * a ResponseWrapper holding the response or the converted exception.
     *
     * @param upstream the upstream response Observable, typically as emitted by a Retrofit client.
     * @return the composed Observable emitting a ResponseWrapper and optionally delayed for network availability
     */
    override fun apply(upstream: Observable<Any>): Observable<ResponseWrapper<O, T, E>> {
        val fetchDate = dateFactory(null)

        @Suppress("UNCHECKED_CAST")
        val networkToken = RequestToken(
                instructionToken.instruction,
                NETWORK,
                fetchDate
        ) as T

        return upstream.map { toResponseWrapper(it, networkToken) }
                .switchIfEmpty(emptyWrapperObservable(networkToken, fetchDate))
                .onErrorResumeNext(Function { errorObservable(it, networkToken) })!!
    }

    private fun emptyWrapperObservable(requestToken: InstructionToken<O>,
                                       fetchDate: Date): Observable<ResponseWrapper<O, T, E>> =
            Observable.defer {
                emptyResponseWrapperFactory.create<O, T>(requestToken, fetchDate.time).observable()//TODO check this
            }

    private fun errorObservable(throwable: Throwable,
                                networkToken: T) =
            ResponseWrapper(
                    responseClass,
                    null,
                    errorFactory.newMetadata(
                            networkToken,
                            errorFactory(throwable)
                    )
            ).observable()

    private fun toResponseWrapper(it: Any,
                                  networkToken: T) =
            if (it is ResponseWrapper<*, *, *>)
                @Suppress("UNCHECKED_CAST")
                it as ResponseWrapper<O, T, E>
            else
                ResponseWrapper(
                        responseClass,
                        it,
                        errorFactory.newMetadata(networkToken)
                )

    class Factory<E>(private val context: Context,
                     private val errorFactory: ErrorFactory<E>,
                     private val emptyResponseWrapperFactory: EmptyResponseWrapperFactory<E>,
                     private val logger: Logger,
                     private val dateFactory: (Long?) -> Date)
            where E : Exception,
                  E : NetworkErrorPredicate {

        fun <O : Remote> create(instructionToken: InstructionToken<O>) = ErrorInterceptor(
                context,
                errorFactory,
                emptyResponseWrapperFactory,
                logger,
                dateFactory,
                instructionToken
        )
    }
}
