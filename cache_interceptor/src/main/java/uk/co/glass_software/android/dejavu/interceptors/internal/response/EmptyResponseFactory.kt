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

package uk.co.glass_software.android.dejavu.interceptors.internal.response

import io.reactivex.Observable
import uk.co.glass_software.android.dejavu.configuration.ErrorFactory
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheStatus
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.response.ResponseWrapper

internal class EmptyResponseFactory<E>(private val errorFactory: ErrorFactory<E>)
        where E : Exception,
              E : NetworkErrorProvider {

    fun emptyResponseWrapperObservable(instructionToken: CacheToken) =
            instructionToken.instruction.let {
                ResponseWrapper(
                        it.responseClass,
                        null,
                        CacheMetadata(
                                instructionToken.copy(
                                        status = CacheStatus.EMPTY,
                                        instruction = it
                                ),
                                errorFactory.getError(NoSuchElementException("Response was empty"))
                        )
                )
            }.let { Observable.just(it) }!!

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
}