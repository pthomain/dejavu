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

package dev.pthomain.android.dejavu.interceptors.error

import android.content.Context
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.boilerplate.core.utils.rx.observable
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.error.error.ErrorFactory
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseFactory
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
 * @param emptyResponseFactory factory providing empty response wrappers
 * @param logger a Logger instance
 * @param dateFactory a factory converting timestamps to Dates
 * @param instructionToken the original request's instruction token
 */
internal class ErrorInterceptor<E>(private val context: Context,
                                   private val errorFactory: ErrorFactory<E>,
                                   private val emptyResponseFactory: EmptyResponseFactory<E>,
                                   private val logger: Logger,
                                   private val dateFactory: (Long?) -> Date,
                                   private val instructionToken: CacheToken)
    : ObservableTransformer<Any, ResponseWrapper<E>>
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
    override fun apply(upstream: Observable<Any>) = upstream
            .map {
                if (it is ResponseWrapper<*>)
                    @Suppress("UNCHECKED_CAST")
                    it as ResponseWrapper<E>
                else
                    ResponseWrapper(
                            responseClass,
                            it,
                            errorFactory.newMetadata(instructionToken)
                    )
            }
            .switchIfEmpty(Observable.defer {
                emptyResponseFactory.emptyResponseWrapper(instructionToken).observable()
            })
            .onErrorResumeNext(Function {
                ResponseWrapper(
                        responseClass,
                        null,
                        errorFactory.newMetadata(
                                instructionToken,
                                errorFactory(it)
                        )
                ).observable()
            })!!

}
