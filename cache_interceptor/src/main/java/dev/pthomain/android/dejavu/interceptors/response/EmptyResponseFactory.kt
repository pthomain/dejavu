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

package dev.pthomain.android.dejavu.interceptors.response

import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.DONE
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.EMPTY
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.error.ErrorFactory
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate

/**
 * Provides empty responses for operations that do not return data (e.g. INVALIDATE or CLEAR), for
 * calls that could return data but had none (OFFLINE) or for network calls that failed.
 *
 * @param errorFactory the custom error factory used to wrap the exception
 */
internal class EmptyResponseFactory<E>(private val errorFactory: ErrorFactory<E>)
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * Returns a Single emitting a ResponseWrapper with no response and a status of
     * either DONE or EMPTY.
     *
     * @param instructionToken the instruction token for this call
     * @return an empty ResponseWrapper emitting Single
     */
    fun emptyResponseWrapper(instructionToken: CacheToken) =
            with(instructionToken) {
                instruction.operation.type.isCompletable.let { isDone ->
                    ResponseWrapper(
                            instruction.requestMetadata.responseClass,
                            null,
                            errorFactory.newMetadata(
                                    copy(status = if (isDone) DONE else EMPTY),
                                    errorFactory(
                                            if (isDone) DoneException(instruction.operation)
                                            else EmptyResponseException
                                    )
                            )
                    )
                }
            }

    object EmptyResponseException : NoSuchElementException("The response was empty")
    data class DoneException(val operation: Operation) : NoSuchElementException("This operation does not return any data: ${operation.type}")
}