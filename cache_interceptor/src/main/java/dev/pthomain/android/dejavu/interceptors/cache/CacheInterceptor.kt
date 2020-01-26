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

package dev.pthomain.android.dejavu.interceptors.cache

import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local.Invalidate
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.DoNotCache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.NOT_CACHED
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.InstructionToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.interceptors.error.ErrorInterceptor
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
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
 * @param errorInterceptor the interceptor dealing with network error handling
 * @param cacheManager an instance of the CacheManager to handle cache operations if needed
 * @param dateFactory provides the current time and converts timestamps
 * @param instructionToken the call specific cache instruction token
 */
internal class CacheInterceptor<O : Operation, T : RequestToken<O>, E> private constructor(
        private val errorInterceptor: ErrorInterceptor<O, T, E>,
        private val cacheManager: CacheManager<E>,
        private val dateFactory: (Long?) -> Date,
        private val instructionToken: InstructionToken<O>,
        private val start: Long
) : ObservableTransformer<ResponseWrapper<O, T, E>, ResponseWrapper<O, T, E>>
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * Composes the given input observable and returns a decorated instance of the same type.
     * @param upstream the upstream Observable instance
     * @return the transformed ObservableSource instance
     */
    @Suppress("UNCHECKED_CAST")
    override fun apply(upstream: Observable<ResponseWrapper<O, T, E>>) =
            with(instructionToken.instruction) {
                when (operation) {
                    is Cache -> cacheManager.getCachedResponse(
                            upstream as Observable<ResponseWrapper<Cache, RequestToken<Cache>, E>>,
                            instructionToken.asInstruction(),
                            start
                    )

                    is Clear -> cacheManager.clearCache(instructionToken.asInstruction()) //TODO update request metadata with type defined in operation

                    is Invalidate -> cacheManager.invalidate(instructionToken.asInstruction()) //TODO update request metadata with type defined in operation

                    else -> doNotCache(
                            instructionToken.asInstruction(),
                            upstream as Observable<ResponseWrapper<DoNotCache, RequestToken<DoNotCache>, E>>
                    )
                }
            }.compose(errorInterceptor)

    /**
     * Indicates that the call has not been cached, either by instruction or because the cache
     * is globally disabled.
     *
     * @param upstream the upstream Observable instance
     * @return the upstream Observable updating the response with the NOT_CACHED status
     */
    private fun doNotCache(instructionToken: InstructionToken<DoNotCache>,
                           upstream: Observable<ResponseWrapper<DoNotCache, RequestToken<DoNotCache>, E>>) =
            upstream.doOnNext { responseWrapper ->
                val requestToken = instructionToken.asRequest<DoNotCache>()
                val instruction = requestToken.instruction

                @Suppress("UNCHECKED_CAST")
                responseWrapper.metadata = responseWrapper.metadata.copy(
                        ResponseToken(
                                instruction,
                                NOT_CACHED,
                                dateFactory(null),
                                null,
                                null
                        )
                )
            }

    class Factory<E>(private val dateFactory: (Long?) -> Date,
                     private val cacheManager: CacheManager<E>)
            where E : Exception,
                  E : NetworkErrorPredicate {

        fun <O : Remote, T : RequestToken<O>> create(errorInterceptor: ErrorInterceptor<O, T, E>,
                                                     instructionToken: InstructionToken<O>,
                                                     start: Long) =
                CacheInterceptor(
                        errorInterceptor,
                        cacheManager,
                        dateFactory,
                        instructionToken,
                        start
                )
    }
}
