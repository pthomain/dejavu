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

package dev.pthomain.android.dejavu.retrofit

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.DejaVu
import dev.pthomain.android.dejavu.DejaVu.Companion.DejaVuHeader
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.configuration.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.configuration.instruction.CacheOperationSerialiser
import dev.pthomain.android.dejavu.configuration.instruction.Operation
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.Request
import retrofit2.Call
import retrofit2.CallAdapter
import java.lang.reflect.Type

/**
 * Retrofit call adapter composing with DejaVuInterceptor. It takes a type {@code E} for the exception
 * used in generic error handling.
 *
 * @see dev.pthomain.android.dejavu.configuration.error.ErrorFactory
 */
internal class RetrofitCallAdapter<E>(private val dejaVuConfiguration: DejaVuConfiguration<E>,
                                      private val responseClass: Class<*>,
                                      private val dejaVuFactory: DejaVuInterceptor.Factory<E>,
                                      private val serialiser: CacheOperationSerialiser,
                                      private val requestBodyConverter: (Request) -> String?,
                                      private val logger: Logger,
                                      private val methodDescription: String,
                                      private val operation: Operation?,
                                      private val rxCallAdapter: CallAdapter<Any, Any>)
    : CallAdapter<Any, Any>
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * Returns the value type as defined by the default RxJava adapter.
     */
    override fun responseType(): Type = rxCallAdapter.responseType()

    /**
     * Adapts the call by composing it with a DejaVuInterceptor if a cache operation is provided
     * or via the default RxJava call adapter otherwise.
     *
     * @return the call adapted to RxJava type
     */
    override fun adapt(call: Call<Any>): Any {
        val header = call.request().header(DejaVuHeader)

        return when {
            operation != null -> {
                if (header != null) adaptedByHeader(
                        call,
                        header,
                        true
                )
                else adaptedWithOperation(
                        call,
                        operation,
                        "Using an operation defined by a call annotation"
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
        val metadata = getRequestMetadata(call)
        return if (dejaVuConfiguration.cachePredicate(responseClass, metadata)) {
            adaptedWithOperation(
                    call,
                    Operation.Expiring.Cache(
                            dejaVuConfiguration.cacheDurationInMillis,
                            dejaVuConfiguration.connectivityTimeoutInMillis,
                            false,
                            dejaVuConfiguration.mergeOnNextOnError,
                            dejaVuConfiguration.encrypt,
                            dejaVuConfiguration.compress,
                            false
                    ),
                    "Using default caching for call with no operation by annotation or header but matching the defined caching predicate"
            )
        } else null
    }

    /**
     * Returns the adapted call composed with a DejaVuInterceptor with a given cache operation,
     * either processed by the annotation processor or set on the call as a header operation.
     *
     * @param call the call to adapt
     * @param operation the cache operation
     * @param source the operation's source
     *
     * @see dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor
     * @see DejaVu.DejaVuHeader
     *
     * @return the call adapted to RxJava type
     */
    private fun adaptedWithOperation(call: Call<Any>,
                                     operation: Operation,
                                     source: String): Any {
        logger.d(
                this,
                "$source for $methodDescription: $operation"
        )

        return adaptRxCall(
                call,
                operation,
                rxCallAdapter.adapt(call)
        )
    }

    /**
     * Returns the adapted call composed with a DejaVuInterceptor with a given header operation.
     *
     * @param call the call to adapt
     * @param header the serialised header
     * @param isOverridingAnnotation whether or not the call also had annotations, in which case the header operation takes precedence over them.
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
                    "WARNING: $methodDescription contains a cache operation defined BOTH by annotation and by header."
                            + " The header operation will take precedence."
            )
        }

        /**
         * Called if the header deserialisation fails, will try to fall back to annotation operation
         * if present or to the default adapter otherwise.
         */
        fun deserialisationFailed() =
                if (operation != null) {
                    logger.e(
                            this,
                            "Found a header cache operation on $methodDescription but it could not be deserialised."
                                    + " The annotation operation will be used instead."
                    )
                    adaptedWithOperation(
                            call,
                            operation,
                            "Using an operation defined by a call annotation"
                    )
                } else {
                    logger.e(
                            this,
                            "Found a header cache operation on $methodDescription but it could not be deserialised."
                    )
                    defaultAdaptation(call)
                }

        return try {
            serialiser.deserialise(header)?.let {
                adaptedWithOperation(
                        call,
                        it,
                        "Using an operation defined by a call header"
                )
            } ?: deserialisationFailed()
        } catch (e: Exception) {
            deserialisationFailed()
        }
    }

    /**
     * Composes the call with DejaVuInterceptor with a given cache operation if possible.
     * Otherwise returns the call adapted with the default RxJava call adapter.
     *
     * @param call the call to be adapted
     * @param operation the cache operation
     * @param adapted the call adapted via the default RxJava call adapter
     *
     * @return the call adapted according to the cache operation
     */
    private fun adaptRxCall(call: Call<Any>,
                            operation: Operation,
                            adapted: Any) =
            when (adapted) {
                is Observable<*> -> adapted.compose(getDejaVuInterceptor(call, operation))
                is Single<*> -> adapted.compose(getDejaVuInterceptor(call, operation))
                is Completable -> adapted.compose(getDejaVuInterceptor(call, operation))
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
                "No annotation or header cache operation found for $methodDescription,"
                        + " the call will be adapted with the default RxJava 2 adapter."
        )
        return rxCallAdapter.adapt(call)
    }

    /**
     * Returns a DejaVuInterceptor for the given cache operation.
     *
     * @param call the call to adapt
     * @param operation the cache operation
     *
     * @return the resulting DejaVuInterceptor
     */
    private fun getDejaVuInterceptor(call: Call<Any>,
                                     operation: Operation) =
            dejaVuFactory.create(
                    operation,
                    getRequestMetadata(call)
            )

    /**
     * Provides metadata for the request, based on the URL and request's body to ensure
     * that calls with the same parameters are considered unique.
     *
     * @param call the call for which to return request metadata
     * @return the request metadata
     */
    private fun getRequestMetadata(call: Call<Any>) =
            RequestMetadata.UnHashed(
                    responseClass,
                    call.request().url().toString(),
                    requestBodyConverter(call.request())
            )

}