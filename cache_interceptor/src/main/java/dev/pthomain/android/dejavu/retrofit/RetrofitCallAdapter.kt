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
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.OperationSerialiser
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.retrofit.RetrofitCallAdapter.Method.*
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
 * @see dev.pthomain.android.dejavu.interceptors.error.error.ErrorFactory
 */
internal class RetrofitCallAdapter<E>(private val dejaVuConfiguration: DejaVuConfiguration<E>,
                                      private val responseClass: Class<*>,
                                      private val dejaVuFactory: DejaVuInterceptor.Factory<E>,
                                      private val serialiser: OperationSerialiser,
                                      private val requestBodyConverter: (Request) -> String?,
                                      private val logger: Logger,
                                      private val methodDescription: String,
                                      private val isWrapped: Boolean,
                                      private val annotationOperation: Operation?,
                                      private val rxCallAdapter: CallAdapter<Any, Any>)
    : CallAdapter<Any, Any>
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * Adapts the call by composing it with a DejaVuInterceptor if a cache operation is provided
     * via any of the supported methods (cache predicate, header or annotation)
     * or via the default RxJava call adapter otherwise.
     *
     * The priority for cache operations is, in decreasing order:
     * - operations returned by the cache predicate for the given RequestMetadata
     * - operations defined in the request's DejaVu header
     * - operations annotated on the request's call
     *
     * N.B: if a call operation is defined using more than one method, only the operation
     * provided via the method with the highest priority is used. The other operations are ignored.
     * For instance, if a call is annotated with a @Cache annotation but the cache predicate
     * returns a DoNotCache operation for its associated request metadata, then the DoNotCache
     * operation takes precedence.
     * @see DejaVuConfiguration.Builder.withPredicate()
     *
     * @param call the current Retrofit call
     * @return the call adapted to RxJava type
     */
    override fun adapt(call: Call<Any>): Any {
        val requestMetadata = RequestMetadata.Plain(
                responseClass,
                call.request().url().toString(),
                requestBodyConverter(call.request())
        )

        val (operation, method) =
                getPredicateOperation(requestMetadata)?.let { it to PREDICATE }
                        ?: getHeaderOperation(call)?.let { it to HEADER }
                        ?: getAnnotationOperation()?.let { it to ANNOTATION }
                        ?: null to null

        return if (operation == null) {
            logger.d(
                    this,
                    "No cache operation found for $methodDescription,"
                            + " the call will be adapted with the default RxJava adapter."
            )
            rxCallAdapter.adapt(call)
        } else {
            adaptedWithOperation(
                    call,
                    operation,
                    requestMetadata,
                    method!!
            )
        }
    }

    /**
     * @param requestMetadata the RequestMetadata for the current call
     * @return the operation returned by the cache predicate for the given RequestMetadata, if any.
     */
    private fun getPredicateOperation(requestMetadata: RequestMetadata): Operation.Remote? {
        logger.d(this, "Checking cache predicate on $methodDescription")
        return dejaVuConfiguration.cachePredicate(requestMetadata)
    }

    /**
     * @param call the current Retrofit call
     * @return the operation deserialised from the call's DejavuHeader, if present and valid.
     */
    private fun getHeaderOperation(call: Call<Any>): Operation? {
        logger.d(this, "Checking cache header on $methodDescription")
        val header = call.request().header(DejaVuHeader) ?: return null

        val operation = try {
            serialiser.deserialise(header)
        } catch (e: Exception) {
            null
        }

        return operation?.also {
            logger.e(
                    this,
                    "Found a header cache operation on $methodDescription but it could not be deserialised."
            )
        }
    }

    /**
     * @return the call's annotated operation if present.
     */
    private fun getAnnotationOperation(): Operation? {
        logger.d(this, "Checking the call's annotations on $methodDescription")
        return annotationOperation
    }

    /**
     * Returns the adapted call composed with a DejaVuInterceptor with a given cache operation,
     * either processed by the annotation processor or set on the call as a header operation.
     *
     * @param call the call to adapt
     * @param operation the cache operation
     * @param requestMetadata the call's associated RequestMetadata
     * @param method the method providing the operation
     *
     * @see dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor
     * @see DejaVu.DejaVuHeader
     *
     * @return the call adapted to RxJava type
     */
    private fun adaptedWithOperation(call: Call<Any>,
                                     operation: Operation,
                                     requestMetadata: RequestMetadata.Plain,
                                     method: Method): Any {
        when (method) {
            PREDICATE -> "the cache predicate"
            HEADER -> "the request's DejaVuHeader"
            ANNOTATION -> "the call's cache annotation"
        }.let {
            logger.d(
                    this,
                    "Found the following operation using $it for $methodDescription: $operation"
            )
        }

        val interceptor = dejaVuFactory.create(
                isWrapped,
                operation,
                requestMetadata
        )

        return with(rxCallAdapter.adapt(call)) {
            when (this) {
                is Single<*> -> compose(interceptor)
                is Observable<*> -> compose(interceptor)
                else -> this
            }
        }
    }

    /**
     * @return the value type as defined by the default RxJava adapter.
     */
    override fun responseType(): Type = rxCallAdapter.responseType()

    private enum class Method {
        PREDICATE,
        HEADER,
        ANNOTATION
    }

}
