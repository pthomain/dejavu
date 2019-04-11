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

package uk.co.glass_software.android.dejavu.retrofit

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Call
import retrofit2.CallAdapter
import uk.co.glass_software.android.boilerplate.core.utils.log.Logger
import uk.co.glass_software.android.dejavu.DejaVu
import uk.co.glass_software.android.dejavu.DejaVu.Companion.DejaVuHeader
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstructionSerialiser
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.DejaVuInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.RequestMetadata
import java.lang.reflect.Type

/**
 * Retrofit call adapter composing with DejaVuInterceptor. It takes a type {@code E} for the exception
 * used in generic error handling.
 *
 * @see uk.co.glass_software.android.dejavu.configuration.ErrorFactory
 */
internal class RetrofitCallAdapter<E>(private val cacheConfiguration: CacheConfiguration<E>,
                                      private val responseClass: Class<*>,
                                      private val dejaVuFactory: DejaVuInterceptor.Factory<E>,
                                      private val serialiser: CacheInstructionSerialiser,
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
     * Adapts the call by composing it with a DejaVuInterceptor if a cache instruction is provided
     * or via the default RxJava call adapter otherwise.
     *
     * @return the call adapted to RxJava type
     */
    override fun adapt(call: Call<Any>): Any {
        val header = call.request().header(DejaVuHeader)

        return when {
            instruction != null -> {
                if (header != null) adaptedByHeader(
                        call,
                        header,
                        true
                )
                else adaptedWithInstruction(
                        call,
                        instruction,
                        "Using an instruction defined by a call annotation"
                )
            }

            header != null -> adaptedByHeader(
                    call,
                    header,
                    false
            )

            else -> defaultAdaptation(call)
        }
    }

    private fun defaultAdaptation(call: Call<Any>) =
            adaptedByDefault(call) ?: adaptedByDefaultRxJavaAdapter(call)

    private fun adaptedByDefault(call: Call<Any>): Any? {
        val metadata = RequestMetadata.UnHashed(call.request().url().toString())
        return if (cacheConfiguration.cachePredicate(responseClass, metadata)) {
            CacheInstruction(
                    responseClass,
                    CacheInstruction.Operation.Expiring.Cache(
                            cacheConfiguration.cacheDurationInMillis,
                            cacheConfiguration.connectivityTimeoutInMillis,
                            false,
                            cacheConfiguration.mergeOnNextOnError,
                            cacheConfiguration.encrypt,
                            cacheConfiguration.compress,
                            false
                    )
            ).let {
                adaptedWithInstruction(
                        call,
                        it,
                        "Using default caching for call with no instruction by annotation or header but matching the defined caching predicate"
                )
            }
        } else null
    }

    /**
     * Returns the adapted call composed with a DejaVuInterceptor with a given cache instruction,
     * either processed by the annotation processor or set on the call as a header instruction.
     *
     * @param call the call to adapt
     * @param instruction the cache instruction
     * @param isFromHeader whether or not the instruction was processed from annotations or header
     *
     * @see uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor
     * @see DejaVu.DejaVuHeader
     *
     * @return the call adapted to RxJava type
     */
    private fun adaptedWithInstruction(call: Call<Any>,
                                       instruction: CacheInstruction,
                                       source: String): Any {
        logger.d(
                this,
                "$source for $methodDescription: $instruction"
        )

        return adaptRxCall(
                call,
                instruction,
                rxCallAdapter.adapt(call)
        )
    }

    /**
     * Returns the adapted call composed with a DejaVuInterceptor with a given header instruction.
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
        logger.d(this, "Checking cache header on $methodDescription")

        if (isOverridingAnnotation) {
            logger.e(
                    this,
                    "WARNING: $methodDescription contains a cache instruction defined BOTH by annotation and by header."
                            + " The header instruction will take precedence."
            )
        }

        /**
         * Called if the header deserialisation fails, will try to fall back to annotation instruction
         * if present or to the default adapter otherwise.
         */
        fun deserialisationFailed() =
                if (instruction != null) {
                    logger.e(
                            this,
                            "Found a header cache instruction on $methodDescription but it could not be deserialised."
                                    + " The annotation instruction will be used instead."
                    )
                    adaptedWithInstruction(
                            call,
                            instruction,
                            "Using an instruction defined by a call annotation"
                    )
                } else {
                    logger.e(
                            this,
                            "Found a header cache instruction on $methodDescription but it could not be deserialised."
                    )
                    defaultAdaptation(call)
                }

        return try {
            serialiser.deserialise(header)?.let {
                adaptedWithInstruction(
                        call,
                        it,
                        "Using an instruction defined by a call header"
                )
            } ?: deserialisationFailed()
        } catch (e: Exception) {
            deserialisationFailed()
        }
    }

    /**
     * Composes the call with DejaVuInterceptor with a given cache instruction if possible.
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
                            adapted: Any) =
            when (adapted) {
                is Observable<*> -> adapted.compose(getDejaVuInterceptor(call, instruction))
                is Single<*> -> adapted.compose(getDejaVuInterceptor(call, instruction))
                is Completable -> adapted.compose(getDejaVuInterceptor(call, instruction))
                else -> adapted
            }

    /**
     * Returns the call adapted with the default RxJava call adapter.
     *
     * @param call the call to adapt
     *
     * @return the call adapted with the default adapter
     */
    private fun adaptedByDefaultRxJavaAdapter(call: Call<Any>): Any {
        logger.d(
                this,
                "No annotation or header cache instruction found for $methodDescription,"
                        + " the call will be adapted with the default RxJava 2 adapter."
        )
        return rxCallAdapter.adapt(call)
    }

    /**
     * Returns a DejaVuInterceptor for the given cache instruction.
     *
     * @param call the call to adapt
     * @param instruction the cache instruction
     *
     * @return the resulting DejaVuInterceptor
     */
    private fun getDejaVuInterceptor(call: Call<Any>,
                                     instruction: CacheInstruction) =
            dejaVuFactory.create(
                    instruction,
                    RequestMetadata.UnHashed(call.request().url().toString())
            )
}
