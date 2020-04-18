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

package dev.pthomain.android.dejavu.interceptors.response

import dev.pthomain.android.dejavu.cache.metadata.response.CallDuration
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.response.Empty
import dev.pthomain.android.dejavu.cache.metadata.response.Result
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.DONE
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import io.reactivex.Observable
import java.util.*

/**
 * Provides empty responses for operations that do not return data (e.g. INVALIDATE or CLEAR), for
 * calls that could return data but had none (OFFLINE) or for network calls that failed.
 *
 * @param errorFactory the custom error factory used to wrap the exception
 */
internal class EmptyResponseFactory<E>(
        private val errorFactory: ErrorFactory<E>
) where E : Throwable,
        E : NetworkErrorPredicate {

    /**
     * TODO JavaDoc
     * Returns a Single emitting a ResponseWrapper with no response and a status of
     * either DONE or EMPTY.
     *
     * @param networkToken the instruction token for this call
     * @return an empty ResponseWrapper emitting Single
     */
    fun <R : Any> createEmptyResponse(networkToken: RequestToken<Cache, R>) =
            Empty(
                    errorFactory(EmptyResponseException(NullPointerException())),
                    networkToken,
                    CallDuration(0, 0, 0) //FIXME
            )

    /**
     * Returns a Single emitting a ResponseWrapper with no response and a status of
     * either DONE or EMPTY.
     *
     * @param networkToken the instruction token for this call
     * @return an empty ResponseWrapper emitting Single
     */
    fun <R : Any, O : Local> createDoneResponse(networkToken: RequestToken<O, R>) =
            Result(
                    RequestToken(
                            networkToken.instruction,
                            DONE,
                            networkToken.requestDate
                    ),
                    CallDuration(0, 0, 0) //FIXME
            )

    /**
     * Wraps a callable action into an Observable that only emits an empty ResponseWrapper (with a DONE status).
     *
     * @param instructionToken the original request's instruction token
     * @param action the callable action to execute as an Observable
     *
     * @return an Observable emitting an empty ResponseWrapper (with a DONE status)
     */
    fun <R : Any, O : Operation> createEmptyResponseObservable(
            instructionToken: RequestToken<O, R>,
            action: () -> Unit = {}
    ) =
            Observable.defer {
                Observable.just(
                        when (instructionToken.instruction.operation) {
                            is Cache -> createEmptyResponse(instructionToken as RequestToken<Cache, R>)
                            else -> createDoneResponse(instructionToken as RequestToken<out Local, R>)
                        } as DejaVuResult<R>
                ).doOnSubscribe { action() }
            }

    class EmptyResponseException(override val cause: Exception) : NoSuchElementException("The response was empty")
    class DoneException(val operation: Operation) : NoSuchElementException("This operation does not return any data: $operation")
}