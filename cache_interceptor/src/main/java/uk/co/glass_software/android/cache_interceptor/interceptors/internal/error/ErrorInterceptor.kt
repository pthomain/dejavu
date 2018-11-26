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

package uk.co.glass_software.android.cache_interceptor.interceptors.internal.error

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.Function
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.boilerplate.utils.rx.RxIgnore
import uk.co.glass_software.android.boilerplate.utils.rx.waitForNetwork
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.DoNotCache
import uk.co.glass_software.android.cache_interceptor.configuration.ErrorFactory
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.AnnotationProcessor
import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.AnnotationProcessor.RxType.COMPLETABLE
import java.util.concurrent.TimeUnit

internal class ErrorInterceptor<E> constructor(private val errorFactory: ErrorFactory<E>,
                                               private val logger: Logger,
                                               private val instructionToken: CacheToken,
                                               private val start: Long,
                                               private val rxType: AnnotationProcessor.RxType,
                                               private val timeOutInSeconds: Int)
    : ObservableTransformer<Any, ResponseWrapper<E>>
        where E : Exception,
              E : NetworkErrorProvider {

    override fun apply(upstream: Observable<Any>) = upstream
            .filter { it != null } //see https://github.com/square/retrofit/issues/2242
            .timeout(timeOutInSeconds.toLong(), TimeUnit.SECONDS) //fixing timeout not working in OkHttp
            .switchIfEmpty {
                if (rxType == COMPLETABLE) RxIgnore.observable()
                else Observable.error(NoSuchElementException("Response was empty"))
            }
            .map { response ->
                val token = instructionToken.let {
                    if (response == RxIgnore)
                        it.copy(instruction = instructionToken.instruction.copy(operation = DoNotCache))
                    else it
                }
                ResponseWrapper<E>(
                        instructionToken.instruction.responseClass,
                        response,
                        CacheMetadata(
                                token,
                                null,
                                getCallDuration()
                        )
                )
            }
            .compose { addConnectivityTimeOutIfNeeded(instructionToken.instruction, it) }
            .onErrorResumeNext(Function { Observable.just(getErrorResponse(it)) })!!

    private fun addConnectivityTimeOutIfNeeded(instruction: CacheInstruction,
                                               upstream: Observable<ResponseWrapper<E>>) =
            instruction.operation.let {
                if (it is CacheInstruction.Operation.Expiring && it.connectivityTimeoutInMillis > 0L) {
                    upstream.waitForNetwork()
                            .timeout(it.connectivityTimeoutInMillis, TimeUnit.MILLISECONDS)
                } else upstream
            }

    private fun getErrorResponse(throwable: Throwable): ResponseWrapper<E> {
        val apiError = errorFactory.getError(throwable)
        val responseClass = instructionToken.instruction.responseClass

        logger.e(
                this,
                apiError,
                "An error occurred during the network request for $responseClass"
        )

        return ResponseWrapper(
                responseClass,
                null,
                CacheMetadata(
                        instructionToken,
                        apiError,
                        getCallDuration()
                )
        )
    }

    private fun getCallDuration() =
            CacheMetadata.Duration(
                    0,
                    (System.currentTimeMillis() - start).toInt(),
                    0
            )
}
