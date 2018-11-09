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

/**
 * Retrofit call adapter composing with RxCacheInterceptor. It takes a type {@code E} for the exception
 * used in generic error handling.
 *
 * @see uk.co.glass_software.android.cache_interceptor.configuration.ErrorFactory
 */
internal class RetrofitCacheAdapter<E>(private val rxCacheFactory: RxCacheInterceptor.Factory<E>,
                                       private val logger: Logger,
                                       private val methodDescription: String,
                                       private val instruction: CacheInstruction?,
                                       private val rxCallAdapter: CallAdapter<Any, Any>)
    : CallAdapter<Any, Any>
        where E : Exception,
              E : NetworkErrorProvider {

    /**
     * Returns the value type as defined by the default RxJava adapter.
     */
    override fun responseType(): Type = rxCallAdapter.responseType()

    /**
     * Adapts the call by composing it with a RxCacheInterceptor if a cache instruction is provided
     * or via the default RxJava call adapter otherwise.
     *
     * @return the call adapted to RxJava type
     */
    override fun adapt(call: Call<Any>): Any {
        val header = call.request().header(RxCache.RxCacheHeader)

        return when {
            instruction != null -> if (header != null) adaptedByHeader(call, header, true)
            else adaptedWithInstruction(call, instruction, false)

            header != null -> adaptedByHeader(call, header, false)

            else -> adaptedByDefaultRxJavaAdapter(call)
        }
    }

    /**
     * Returns the adapted call composed with a RxCacheInterceptor with a given cache instruction,
     * either processed by the annotation processor or set on the call as a header instruction.
     *
     * @param call the call to adapt
     * @param instruction the cache instruction
     * @param isFromHeader whether or not the instruction was processed from annotations or header
     *
     * @see uk.co.glass_software.android.cache_interceptor.retrofit.annotations.AnnotationProcessor
     * @see RxCache.RxCacheHeader
     *
     * @return the call adapted to RxJava type
     */
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

    /**
     * Returns the adapted call composed with a RxCacheInterceptor with a given header instruction.
     *
     * @param call the call to adapt
     * @param header the serialised header
     * @param isOverridingAnnotation whether or not the call also had annotations, in which case the header instruction takes precedence over them.
     *
     * @return the call adapted to RxJava type
     */
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

    /**
     * Composes the call with RxCacheInterceptor with a given cache instruction if possible.
     * Otherwise returns the call adapted with the default RxJava call adapter.
     *
     * @param call the call to be adapted
     * @param instruction the cache instruction
     * @param adapted the call adapted via the default RxJava call adapter
     *
     * @return the call adapted according to the cache instruction
     */
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

    /**
     * Returns the call adapted with the default RxJava call adapter.
     *
     * @param call the call to adapt
     *
     * @return the call adapted with the default adapter
     */
    private fun adaptedByDefaultRxJavaAdapter(call: Call<Any>): Any {
        logger.d("No annotation or header cache instruction found for $methodDescription,"
                + " the call will be adapted with the default RxJava 2 adapter."
        )
        return rxCallAdapter.adapt(call)
    }

    /**
     * Returns a RxCacheInterceptor for the given cache instruction.
     *
     * @param call the call to adapt
     * @param instruction the cache instruction
     *
     * @return the resulting RxCacheInterceptor
     */
    private fun getRxCacheInterceptor(call: Call<Any>,
                                      instruction: CacheInstruction) =
            rxCacheFactory.create(
                    instruction,
                    call.request().url().toString(),
                    call.request().body()?.toString()
            )

}
