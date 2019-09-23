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

package dev.pthomain.android.dejavu.interceptors.internal.cache

import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.*
import dev.pthomain.android.dejavu.configuration.NetworkErrorProvider
import dev.pthomain.android.dejavu.interceptors.internal.cache.token.CacheToken
import dev.pthomain.android.dejavu.response.ResponseWrapper
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import java.util.*

/**
 * Class handling the interception of the network request observable with the purpose of decorating
 * it based on the cache operation defined in the associated instruction token.
 *
 * This class delegates cache operations to the CacheManager if needed or update the response wrapper
 * with a NOT_CACHED token otherwise.
 *
 * @param cacheManager an instance of the CacheManager to handle cache operations if needed
 * @param dateFactory provides the current time and converts timestamps
 * @param isCacheEnabled whether or not the global configuration sets the cache as being enabled
 * @param instructionToken the call specific cache instruction token
 * @param start the timestamp indicating when the call started
 */
internal class CacheInterceptor<E>(private val cacheManager: CacheManager<E>,
                                   private val dateFactory: (Long?) -> Date,
                                   private val isCacheEnabled: Boolean,
                                   private val instructionToken: CacheToken,
                                   private val start: Long)
    : ObservableTransformer<ResponseWrapper<E>, ResponseWrapper<E>>
        where E : Exception,
              E : NetworkErrorProvider {

    /**
     * Composes the given input observable and returns a decorated instance of the same type.
     * @param upstream the upstream Observable instance
     * @return the transformed ObservableSource instance
     */
    override fun apply(upstream: Observable<ResponseWrapper<E>>) =
            instructionToken.instruction.let { instruction ->
                if (isCacheEnabled) {
                    when (instruction.operation) {
                        is Expiring -> cacheManager.getCachedResponse(
                                upstream,
                                instructionToken,
                                instruction.operation,
                                start
                        )

                        is Clear -> cacheManager.clearCache(
                                instructionToken,
                                instruction.operation.typeToClear,
                                instruction.operation.clearStaleEntriesOnly
                        )

                        is Invalidate -> cacheManager.invalidate(instructionToken)

                        else -> doNotCache(upstream)
                    }
                } else doNotCache(upstream)
            }!!

    /**
     * Indicates that the call has not been cached, either by instruction or because the cache
     * is globally disabled.
     *
     * @param upstream the upstream Observable instance
     * @return the upstream Observable updating the response with the NOT_CACHED status
     */
    private fun doNotCache(upstream: Observable<ResponseWrapper<E>>) =
            upstream.doOnNext { responseWrapper ->
                responseWrapper.metadata = responseWrapper.metadata.copy(
                        CacheToken.notCached(
                                instructionToken,
                                dateFactory(null)
                        )
                )
            }

}