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

package uk.co.glass_software.android.cache_interceptor.retrofit

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Call
import retrofit2.CallAdapter
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.RxCache
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstructionSerialiser
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import java.lang.reflect.Type

@Suppress("UNCHECKED_CAST")
internal class RetrofitCacheAdapter<E>(private val rxCacheFactory: RxCacheInterceptor.Factory<E>,
                                       private val logger: Logger,
                                       private val methodDescription: String,
                                       private val instruction: CacheInstruction?,
                                       private val rxCallAdapter: CallAdapter<Any, Any>)
    : CallAdapter<Any, Any>
        where E : Exception,
              E : NetworkErrorProvider {

    override fun responseType(): Type = rxCallAdapter.responseType()

    override fun adapt(call: Call<Any>): Any {
        val header = call.request().header(RxCache.RxCacheHeader)

        return when {
            instruction != null -> if (header != null) adaptedByHeader(call, header, true)
            else adaptedWithInstruction(call, instruction, false)

            header != null -> adaptedByHeader(call, header, false)

            else -> adaptedByDefaultRxJavaAdapter(call)
        }
    }

    private fun adaptedWithInstruction(call: Call<Any>,
                                       instruction: CacheInstruction,
                                       isFromHeader: Boolean): Any {
        logger.d("Found ${if (isFromHeader) "a header" else "an annotation"} cache instruction on $methodDescription: $instruction")

        return adaptRxCall(
                call,
                instruction,
                rxCallAdapter.adapt(call)
        )
    }

    private fun adaptedByHeader(call: Call<Any>,
                                header: String,
                                isOverridingAnnotation: Boolean): Any {
        logger.d("Checking cache header on $methodDescription")

        if (isOverridingAnnotation) {
            logger.d("WARNING: $methodDescription contains a cache instruction BOTH by annotation and by header."
                    + " The header instruction will take precedence."
            )
        }

        fun deserialisationFailed() = adaptedByDefaultRxJavaAdapter(call).also {
            logger.e("Found a header cache instruction on $methodDescription but it could not be deserialised."
                    + " This call won't be cached.")
        }

        return try {
            CacheInstructionSerialiser.deserialise(header)?.let {
                adaptedWithInstruction(call, it, true)
            } ?: deserialisationFailed()
        } catch (e: Exception) {
            deserialisationFailed()
        }
    }

    private fun adaptRxCall(call: Call<Any>,
                            instruction: CacheInstruction,
                            adapted: Any): Any {
        val responseClass = instruction.responseClass
        return when (adapted) {
            is Observable<*> -> adapted.cast(responseClass)
                    .compose(getRxCacheInterceptor(call, instruction))

            is Single<*> -> adapted.cast(responseClass)
                    .compose(getRxCacheInterceptor(call, instruction))

            is Completable -> adapted.toObservable<Any>()
                    .cast(responseClass)
                    .compose(getRxCacheInterceptor(call, instruction))
                    .ignoreElements()

            else -> adapted
        }
    }

    private fun adaptedByDefaultRxJavaAdapter(call: Call<Any>): Any {
        logger.d("No annotation or header cache instruction found for $methodDescription,"
                + " the call will be adapted with the default RxJava 2 adapter."
        )
        return rxCallAdapter.adapt(call)
    }

    private fun getRxCacheInterceptor(call: Call<Any>,
                                      instruction: CacheInstruction) =
            rxCacheFactory.create(
                    instruction,
                    call.request().url().toString(),
                    call.request().body()?.toString()
            )

}
