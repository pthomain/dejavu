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
import dev.pthomain.android.dejavu.utils.Logger
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.di.DateFactory
import dev.pthomain.android.dejavu.utils.waitForNetwork
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

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
        private val requestToken: T
) where E : Throwable,
      E : NetworkErrorPredicate {

    /**
     * Intercepts the upstream response Flow, optionally adding request timeout
     * and connectivity timeout behaviour.
     *
     * @param upstream the upstream response Flow, typically as emitted by a Retrofit client.
     * @return the intercepted Flow emitting a DejaVuResult, optionally delayed for network availability
     */
    fun intercept(upstream: Flow<DejaVuResult<R>>): Flow<DejaVuResult<R>> =
            with(requestToken.instruction) {
                if (operation is Cache) {
                    val requestTimeOut = (operation as Cache).requestTimeOutInSeconds
                    val connectivityTimeout = (operation as Cache).connectivityTimeoutInSeconds
                        .let { if (it == null || it == -1) null else it }

                    flow {
                        // Handle connectivity timeout: wait for network before collecting upstream
                        if (connectivityTimeout != null && connectivityTimeout > 0) {
                            withTimeout(connectivityTimeout.toLong().seconds) {
                                context.waitForNetwork()
                            }
                        }

                        // Handle request timeout
                        if (requestTimeOut != null && requestTimeOut > 0) {
                            withTimeout(requestTimeOut.toLong().seconds) {
                                emitAll(upstream)
                            }
                        } else {
                            emitAll(upstream)
                        }
                    }
                } else upstream
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
