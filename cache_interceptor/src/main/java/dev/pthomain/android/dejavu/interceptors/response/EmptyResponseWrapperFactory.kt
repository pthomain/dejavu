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

import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Local
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.DONE
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.EMPTY
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.EmptyRemoteToken
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.error.ErrorFactory
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.error.error.newMetadata
import java.util.*

/**
 * Provides empty responses for operations that do not return data (e.g. INVALIDATE or CLEAR), for
 * calls that could return data but had none (OFFLINE) or for network calls that failed.
 *
 * @param errorFactory the custom error factory used to wrap the exception
 */
class EmptyResponseWrapperFactory<E>(private val errorFactory: ErrorFactory<E>)
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * Returns a Single emitting a ResponseWrapper with no response and a status of
     * either DONE or EMPTY.
     *
     * @param instructionToken the instruction token for this call
     * @return an empty ResponseWrapper emitting Single
     */
    fun create(instructionToken: CacheToken,
               fetchDate: Date) =
            with(instructionToken) {
                ResponseWrapper(
                        instruction.requestMetadata.responseClass,
                        null,
                        errorFactory.newMetadata(
                                EmptyRemoteToken(
                                        instruction,
                                        ifElse(instruction.operation is Local, DONE, EMPTY),
                                        fetchDate
                                ),
                                errorFactory(
                                        if (instruction.operation is Local) DoneException(instruction.operation)
                                        else EmptyResponseException(NullPointerException())//FIXME
                                )
                        )
                )
            }

    class EmptyResponseException(override val cause: Exception) : NoSuchElementException("The response was empty")
    class DoneException(val operation: Operation) : NoSuchElementException("This operation does not return any data: $operation")
}