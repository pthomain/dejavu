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

import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation.*
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.NOT_CACHED
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
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
 * @param isCacheEnabled whether or not the global configuration sets the cache as being enabled
 * @param instructionToken the call specific cache instruction token
 * @param start the timestamp indicating when the call started
 */
internal class CacheInterceptor<E>(private val errorInterceptor: ErrorInterceptor<E>,
                                   private val cacheManager: CacheManager<E>,
                                   private val dateFactory: (Long?) -> Date,
                                   private val isCacheEnabled: Boolean,
                                   private val instructionToken: CacheToken,
                                   private val start: Long)
    : ObservableTransformer<ResponseWrapper<E>,ResponseWrapper<E>>
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * Composes the given input observable and returns a decorated instance of the same type.
     * @param upstream the upstream Observable instance
     * @return the transformed ObservableSource instance
     */
    override fun apply(upstream: Observable<ResponseWrapper<E>>) =
            instructionToken.instruction.let { instruction ->
                if (isCacheEnabled) {
                    when (instruction.operation) {
                        is Cache -> cacheManager.getCachedResponse(
                                upstream,
                                instructionToken,
                                start
                        )

                        is Clear -> cacheManager.clearCache(instructionToken) //TODO update request metadata with type defined in operation

                        is Invalidate -> cacheManager.invalidate(instructionToken) //TODO update request metadata with type defined in operation

                        else -> doNotCache(upstream)
                    }
                } else doNotCache(upstream)
            }.compose(errorInterceptor)

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
                        instructionToken.copy(
                                status = NOT_CACHED,
                                fetchDate = dateFactory(null),
                                cacheDate = null,
                                expiryDate = null
                        )
                )
            }
}
