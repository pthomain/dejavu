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

import dev.pthomain.android.dejavu.configuration.error.ErrorFactory
import dev.pthomain.android.dejavu.configuration.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.configuration.instruction.Operation
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.DONE
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.EMPTY
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import io.reactivex.Single

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
    fun emptyResponseWrapperSingle(instructionToken: CacheToken) =
            instructionToken.instruction.operation.type.isCompletable.let { isDone ->
                Single.just(ResponseWrapper(
                        instructionToken.instruction.requestMetadata.responseClass,
                        null,
                        CacheMetadata(
                                instructionToken.copy(status = if (isDone) DONE else EMPTY),
                                errorFactory(
                                        if (isDone) DoneException(instructionToken.instruction.operation)
                                        else EmptyResponseException
                                )
                        )
                ))
            }!!

    /**
     * Creates an empty response to be returned in lieu of an exception if the mergeOnNextOnError
     * is set to true and the response class implements CacheMetadata.Holder.
     *
     * @param mergeOnNextOnError whether or not any exception should be added to the metadata on an empty response and delivered via onNext. This is only applied if the response implements CacheMetadata.Holder. An exception is thrown otherwise.
     * @param responseClass the target response class
     *
     * @return the empty response if possible
     */
    fun create(mergeOnNextOnError: Boolean,
               responseClass: Class<*>) =
            if (mergeOnNextOnError) {
                try {
                    responseClass.newInstance()
                } catch (e: Exception) {
                    null
                }
            } else null

    object EmptyResponseException : NoSuchElementException("The response was empty")
    class DoneException(operation: Operation) : NoSuchElementException("This operation does not return any data: ${operation.type}")
}