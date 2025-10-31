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
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.token.CacheStatus.NOT_CACHED
import dev.pthomain.android.dejavu.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.CacheInstruction
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Invalidate
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.DoNotCache
import dev.pthomain.android.glitchy.core.interceptor.interceptors.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.flow.interceptors.base.FlowInterceptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

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
        private val requestToken: RequestToken<O, R>,
) : FlowInterceptor()// ObservableTransformer<ResultWrapper<R>, DejaVuResult<R>>
        where E : Throwable,
              E : NetworkErrorPredicate {

    @Suppress("UNCHECKED_CAST")
    override fun flatMap(upstream: Flow<Any>): Flow<Any> = flow {
        when (requestToken.instruction.operation) {
            is Cache -> emitAll(
                    cacheManager.getCachedResponse(
                            upstream as Flow<DejaVuResult<R>>, //This is enforced by Glitchy
                            requestToken as RequestToken<Cache, R>
                    )
            )

            is Clear -> cacheManager.clearCache(requestToken as RequestToken<Clear, R>)

            is Invalidate -> emit(
                    cacheManager.invalidate(requestToken as RequestToken<Invalidate, R>)
            )

            else -> emit(doNotCache(upstream as Response<R, DoNotCache>))
        }
    }

    /**
     * Indicates that the call has not been cached, either by instruction or because the cache
     * is globally disabled.
     *
     * @param upstream the upstream Observable instance
     * @return the upstream Observable updating the response with the NOT_CACHED status
     */
    private fun doNotCache(upstream: Response<R, DoNotCache>): DejaVuResult<R> =
            @Suppress("UNCHECKED_CAST") //bad compiler inference
            upstream.copy(
                    cacheToken = ResponseToken(
                            requestToken.instruction as CacheInstruction<DoNotCache, R>,
                            NOT_CACHED,
                            requestToken.requestDate
                    )
            )

    internal class Factory<E>(
            private val cacheManager: CacheManager<E>
    ) where E : Throwable,
            E : NetworkErrorPredicate {

        fun <R : Any, O : Operation> create(requestToken: RequestToken<O, R>) = CacheInterceptor(
                cacheManager,
                requestToken
        )
    }
}
