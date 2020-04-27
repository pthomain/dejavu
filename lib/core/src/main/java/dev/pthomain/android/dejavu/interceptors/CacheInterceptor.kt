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

package dev.pthomain.android.dejavu.interceptors

import dev.pthomain.android.dejavu.cache.CacheManager
import dev.pthomain.android.dejavu.cache.metadata.response.CallDuration
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.response.ResultWrapper
import dev.pthomain.android.dejavu.shared.token.CacheStatus.NOT_CACHED
import dev.pthomain.android.dejavu.shared.token.RequestToken
import dev.pthomain.android.dejavu.shared.token.ResponseToken
import dev.pthomain.android.dejavu.shared.token.instruction.CacheInstruction
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Local.Invalidate
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Remote.DoNotCache
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import io.reactivex.Observable
import io.reactivex.ObservableTransformer

/**
 * Class handling the interception of the network request observable with the purpose of decorating
 * it based on the cache operation defined in the associated instruction token.
 *
 * This class delegates cache operations to the CacheManager if needed or update the response wrapper
 * with a NOT_CACHED token otherwise.
 *
 * @param cacheManager an instance of the CacheManager to handle cache operations if needed
 */
internal class CacheInterceptor<R : Any, O : Operation, E> private constructor(
        private val cacheManager: CacheManager<E>,
        private val requestToken: RequestToken<O, R>
) : ObservableTransformer<ResultWrapper<R>, DejaVuResult<R>>
        where E : Throwable,
              E : NetworkErrorPredicate {

    /**
     * Composes the given input observable and returns a decorated instance of the same type.
     * @param upstream the upstream Observable instance
     * @return the transformed ObservableSource instance
     */
    @Suppress("UNCHECKED_CAST")
    override fun apply(upstream: Observable<ResultWrapper<R>>): Observable<DejaVuResult<R>> {
        return when (requestToken.instruction.operation) {
            is Cache -> cacheManager.getCachedResponse(
                    upstream.map { it as Response<R, Cache> }, //TODO check this
                    requestToken as RequestToken<Cache, R>
            )

            is Clear -> cacheManager.clearCache(requestToken as RequestToken<Clear, R>) //TODO update request metadata with type defined in operation

            is Invalidate -> cacheManager.invalidate(requestToken as RequestToken<Invalidate, R>) //TODO update request metadata with type defined in operation

            else -> doNotCache(upstream as Observable<Response<R, DoNotCache>>)
        }
    }

    /**
     * Indicates that the call has not been cached, either by instruction or because the cache
     * is globally disabled.
     *
     * @param upstream the upstream Observable instance
     * @return the upstream Observable updating the response with the NOT_CACHED status
     */
    private fun doNotCache(upstream: Observable<Response<R, DoNotCache>>): Observable<DejaVuResult<R>> =
            upstream.map {
                val instruction = requestToken.instruction

                Response(
                        it.response,
                        ResponseToken(
                                instruction as CacheInstruction<DoNotCache, R>,
                                NOT_CACHED,
                                requestToken.requestDate
                        ),
                        CallDuration(0, 0, 0) //FIXME
                )
            }

    internal class Factory<E>(
            private val cacheManager: CacheManager<E>
    ) where E : Throwable,
            E : NetworkErrorPredicate {

        fun <R : Any, O : Operation> create(requestToken: RequestToken<O, R>) =
                CacheInterceptor(cacheManager, requestToken)
    }
}
