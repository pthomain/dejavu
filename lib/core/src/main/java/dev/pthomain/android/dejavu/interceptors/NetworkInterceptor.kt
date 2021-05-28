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

import android.content.Context
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.di.DateFactory
import dev.pthomain.android.glitchy.core.interceptor.interceptors.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.flow.interceptors.base.FlowInterceptor

/**
 * This interceptor adds a connectivity timeout to the network call, which defines a maximum
 * period of time to wait for the network to become available.
 * This delay does not affect the emission of any available cached data (according to the request's
 * cache instruction) but only the network call in the case the data is STALE and the request's
 * instruction warrants FRESH data to be returned (i.e CACHE or REFRESH operation).
 *
 * @param context the application context
 * @param logger a Logger instance
 * @param dateFactory a factory converting timestamps to Dates
 * @param requestToken the original request cache token
 */
internal class NetworkInterceptor<O : Remote, R : Any, T : RequestToken<out O, R>, E> private constructor(
        private val context: Context,
        private val logger: Logger,
        private val dateFactory: DateFactory,
        private val requestToken: T,
) : FlowInterceptor()// ObservableTransformer<DejaVuResult<R>, DejaVuResult<R>>
        where E : Throwable,
              E : NetworkErrorPredicate {

    /**
     * The composition method converting an upstream response Observable to an Observable emitting
     * a ResponseWrapper holding the response or the converted exception.
     *
     * @param upstream the upstream response Observable, typically as emitted by a Retrofit client.
     * @return the composed Observable emitting a ResponseWrapper and optionally delayed for network availability
     */
    //FIXME move to RxJava module
    override suspend fun map(value: Any): Any {
        return value
//        val upstream = value as DejaVuResult<R>
//        return with(requestToken.instruction) {
//            if (operation is Cache) {
//                with((operation as Cache).requestTimeOutInSeconds) {
//                    if (this != null && this > 0) it.timeout(toLong(), SECONDS) //fixing timeout not working in OkHttp
//                    else it
//                }.compose {
//                    (operation as Cache).connectivityTimeoutInSeconds.swapWhenDefault(null)?.let { timeOut ->
//                        upstream.swapLambdaWhen(timeOut > 0L) {
//                            upstream.waitForNetwork(context, logger)
//                                    .timeout(timeOut.toLong(), SECONDS)
//                        }
//                    } ?: it
//                }
//            } else upstream
//        }
    }

    class Factory<E>(
            private val context: Context,
            private val logger: Logger,
            private val dateFactory: DateFactory
    ) where E : Throwable,
            E : NetworkErrorPredicate {

        fun <O : Remote, R : Any, T : RequestToken<out O, R>> create(requestToken: T) =
                NetworkInterceptor<O, R, T, E>(
                        context,
                        logger,
                        dateFactory,
                        requestToken
                )
    }

}
