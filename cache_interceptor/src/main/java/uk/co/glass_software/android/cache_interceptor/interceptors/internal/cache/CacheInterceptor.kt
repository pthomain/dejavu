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

package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.*
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import java.util.*

internal class CacheInterceptor<E> constructor(private val cacheManager: CacheManager<E>,
                                               private val isCacheEnabled: Boolean,
                                               private val logger: Logger,
                                               private val instructionToken: CacheToken,
                                               private val start: Long)
    : ObservableTransformer<ResponseWrapper<E>, ResponseWrapper<E>>
        where E : Exception,
              E : NetworkErrorProvider {

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
                                instruction.operation.clearOldEntriesOnly
                        )

                        is Invalidate -> cacheManager.invalidate(instructionToken)

                        else -> doNotCache(instructionToken, upstream)
                    }
                } else doNotCache(instructionToken, upstream)
            }

    private fun doNotCache(instructionToken: CacheToken,
                           upstream: Observable<ResponseWrapper<E>>) =
            upstream.doOnNext { responseWrapper ->
                responseWrapper.metadata = CacheMetadata(
                        CacheToken.notCached(
                                instructionToken,
                                Date()
                        )
                )
            }

}