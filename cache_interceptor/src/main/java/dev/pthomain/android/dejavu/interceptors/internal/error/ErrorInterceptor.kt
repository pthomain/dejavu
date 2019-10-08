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

package dev.pthomain.android.dejavu.interceptors.internal.error

import android.content.Context
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.boilerplate.core.utils.rx.observable
import dev.pthomain.android.boilerplate.core.utils.rx.waitForNetwork
import dev.pthomain.android.dejavu.configuration.CacheInstruction
import dev.pthomain.android.dejavu.configuration.ErrorFactory
import dev.pthomain.android.dejavu.configuration.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.response.ResponseWrapper
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.Function
import java.util.*
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.NoSuchElementException

/**
 * Interceptor handling network exceptions, converting them using the chose ErrorFactory and
 * returning a ResponseWrapper holding the response or exception.
 *
 * This interceptor also adds a connectivity timeout to the network call, which defines a maximum
 * period of time to wait for the network to become available.
 * This delay does not affect the emission of any available cached data (according to the request's
 * cache instruction) but only the network call in the case the data is STALE and the request's
 * instruction warrants FRESH data to be returned (i.e CACHE or REFRESH operation).
 *
 * @see ErrorFactory
 * @param context the application context
 * @param errorFactory the factory converting throwables to custom exceptions
 * @param logger a Logger instance
 * @param dateFactory a factory converting timestamps to Dates
 * @param instructionToken the original request's instruction token
 * @param start the time at which the request started
 * @param requestTimeOutInSeconds the aforementioned network availability delay
 */
internal class ErrorInterceptor<E>(private val context: Context,
                                   private val errorFactory: ErrorFactory<E>,
                                   private val logger: Logger,
                                   private val dateFactory: (Long?) -> Date,
                                   private val instructionToken: CacheToken,
                                   private val start: Long,
                                   private val requestTimeOutInSeconds: Int)
    : ObservableTransformer<Any, ResponseWrapper<E>>
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * The composition method converting an upstream response Observable to an Observable emitting
     * a ResponseWrapper holding the response or the converted exception.
     *
     * @param upstream the upstream response Observable, typically as emitted by a Retrofit client.
     * @return the composed Observable emitting a ResponseWrapper and optionally delayed for network availability
     */
    override fun apply(upstream: Observable<Any>) = upstream
            .filter { it != null } //see https://github.com/square/retrofit/issues/2242
            .map { wrap(it) }
            .timeout(requestTimeOutInSeconds.toLong(), SECONDS) //fixing timeout not working in OkHttp
            .defaultIfEmpty(wrapError(NoSuchElementException("Response was empty")))
            .onErrorResumeNext(Function { wrapError(it).observable() })
            .compose {
                addConnectivityTimeOutIfNeeded(it)
            }!!

    /**
     * Adds an optional delay for network availability (if the value is set as more than 0).
     *
     * @see dev.pthomain.android.dejavu.configuration.CacheConfiguration.connectivityTimeoutInMillis
     * @param upstream the upstream response Observable, typically as emitted by a Retrofit client.
     * @return the composed Observable optionally delayed for network availability
     */
    private fun addConnectivityTimeOutIfNeeded(upstream: Observable<ResponseWrapper<E>>) =
            instructionToken.instruction.operation.let {
                if (it is CacheInstruction.Operation.Expiring) {
                    val timeOut = it.connectivityTimeoutInMillis ?: 0L
                    if (timeOut > 0L)
                        upstream.waitForNetwork(context, logger)
                                .timeout(timeOut, MILLISECONDS)
                    else upstream
                } else upstream
            }

    /**
     * Wraps a given response in a ResponseWrapper
     *
     * @param response the response to wrap
     * @return a ResponseWrapper holding the given response with some metadata
     */
    private fun wrap(response: Any) = ResponseWrapper<E>(
            instructionToken.instruction.responseClass,
            response,
            CacheMetadata(
                    instructionToken,
                    null,
                    getCallDuration()
            )
    )

    /**
     * Wraps a given throwable in a ResponseWrapper
     *
     * @param throwable the throwable to wrap
     * @return a ResponseWrapper holding the given throwable with some metadata
     */
    private fun wrapError(throwable: Throwable) =
            ResponseWrapper(
                    instructionToken.instruction.responseClass,
                    null,
                    CacheMetadata(
                            instructionToken,
                            errorFactory.getError(throwable),
                            getCallDuration()
                    )
            )

    /**
     * @return a Duration metadata object holding the duration of the network call
     */
    private fun getCallDuration() =
            CacheMetadata.Duration(
                    0,
                    (dateFactory(null).time - start).toInt(),
                    0
            )
}
