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

package uk.co.glass_software.android.dejavu.interceptors.internal.error

import android.content.Context
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.Function
import uk.co.glass_software.android.boilerplate.core.utils.log.Logger
import uk.co.glass_software.android.boilerplate.core.utils.rx.observable
import uk.co.glass_software.android.boilerplate.core.utils.rx.waitForNetwork
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.ErrorFactory
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import java.util.*
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.NoSuchElementException

//TODO JavaDoc
internal class ErrorInterceptor<E>(private val context: Context,
                                   private val errorFactory: ErrorFactory<E>,
                                   private val logger: Logger,
                                   private val dateFactory: (Long?) -> Date,
                                   private val instructionToken: CacheToken,
                                   private val start: Long,
                                   private val requestTimeOutInSeconds: Int)
    : ObservableTransformer<Any, ResponseWrapper<E>>
        where E : Exception,
              E : NetworkErrorProvider {

    override fun apply(upstream: Observable<Any>) = upstream
            .filter { it != null } //see https://github.com/square/retrofit/issues/2242
            .map { wrap(it) }
            .timeout(requestTimeOutInSeconds.toLong(), SECONDS) //fixing timeout not working in OkHttp
            .defaultIfEmpty(getErrorResponse(NoSuchElementException("Response was empty")))
            .onErrorResumeNext(Function { getErrorResponse(it).observable() })
            .compose {
                addConnectivityTimeOutIfNeeded(it)
            }!!

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

    private fun wrap(it: Any) = ResponseWrapper<E>(
            instructionToken.instruction.responseClass,
            it,
            CacheMetadata(
                    instructionToken,
                    null,
                    getCallDuration()
            )
    )

    private fun getErrorResponse(throwable: Throwable) =
            ResponseWrapper(
                    instructionToken.instruction.responseClass,
                    null,
                    CacheMetadata(
                            instructionToken,
                            errorFactory.getError(throwable),
                            getCallDuration()
                    )
            )

    private fun getCallDuration() =
            CacheMetadata.Duration(
                    0,
                    (dateFactory(null).time - start).toInt(),
                    0
            )
}
