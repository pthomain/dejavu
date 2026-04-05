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

import dev.pthomain.android.dejavu.cache.metadata.response.*
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.DONE
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.di.DateFactory
import dev.pthomain.android.dejavu.error.ErrorFactory
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import java.util.*

/**
 * Provides empty responses for operations that do not return data (e.g. INVALIDATE or CLEAR), for
 * calls that could return data but had none (OFFLINE) or for network calls that failed.
 *
 * @param errorFactory the custom error factory used to wrap the exception
 */
internal class EmptyResponseFactory<E>(
        private val errorFactory: ErrorFactory<E>,
        private val dateFactory: DateFactory
) where E : Throwable,
        E : NetworkErrorPredicate {

    /**
     * Returns an Empty DejaVuResult with no response and an EmptyResponseException.
     *
     * @param networkToken the instruction token for this call
     * @return an empty DejaVuResult
     */
    fun <R : Any> createEmptyResponse(networkToken: RequestToken<Cache, R>) =
            Empty(
                    errorFactory(EmptyResponseException(NullPointerException())),
                    networkToken,
                    CallDuration(disk = networkToken.ellapsed(dateFactory))
            )

    /**
     * Returns a Result DejaVuResult with no response and a status of DONE.
     *
     * @param networkToken the instruction token for this call
     * @return a done DejaVuResult
     */
    fun <R : Any, O : Local> createDoneResponse(networkToken: RequestToken<O, R>) =
            Result(
                    RequestToken(
                            networkToken.instruction,
                            DONE,
                            networkToken.requestDate
                    ),
                    CallDuration(disk = networkToken.ellapsed(dateFactory))
            )

    /**
     * Wraps a callable action into a Flow that only emits an empty DejaVuResult (with a DONE status).
     *
     * @param instructionToken the original request's instruction token
     * @param action the callable action to execute before emitting
     *
     * @return a Flow emitting an empty DejaVuResult (with a DONE status)
     */
    fun <R : Any, O : Operation> createEmptyResponseFlow(
            instructionToken: RequestToken<O, R>,
            action: () -> Unit = {}
    ): Flow<DejaVuResult<R>> =
            flow {
                action()
                @Suppress("UNCHECKED_CAST") //This is enforced by CacheInterceptor
                val result = when (instructionToken.instruction.operation) {
                    is Cache -> createEmptyResponse(instructionToken as RequestToken<Cache, R>)
                    else -> createDoneResponse(instructionToken as RequestToken<out Local, R>)
                } as DejaVuResult<R>
                emit(result)
            }

    class EmptyResponseException(override val cause: Exception) : NoSuchElementException("The response was empty")
}
