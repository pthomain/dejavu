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

package dev.pthomain.android.dejavu.retrofit.operation

import dev.pthomain.android.dejavu.utils.Logger
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.PlainRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.RequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.toOperation
import dev.pthomain.android.dejavu.retrofit.operation.RetrofitOperationResolver.Method.*
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate
import okhttp3.Request
import retrofit2.Call

/**
 * Use this value to provide the cache instruction as a header (this will override any existing call annotation)
 */
const val DejaVuHeader = "DejaVuHeader"

/**
 * Resolves the cache operation for a Retrofit call.
 *
 * The priority for cache operations is, in decreasing order:
 * - operations returned by the cache predicate for the given RequestMetadata
 * - operations defined in the request's DejaVu header
 * - operations annotated on the request's call
 *
 * @param responseClass the response class for this call
 * @param requestBodyConverter converts request bodies to Strings for hashing
 * @param operationPredicate predicate that can override the operation for any request
 * @param annotationOperation the operation derived from annotations
 * @param methodDescription a description of the method for logging
 * @param logger the logger
 */
internal class RetrofitOperationResolver<E, R> private constructor(
    private val responseClass: Class<R>,
    private val requestBodyConverter: (Request) -> String?,
    private val operationPredicate: (RequestMetadata<*>) -> Operation.Remote?,
    private val annotationOperation: Operation?,
    private val methodDescription: String,
    private val logger: Logger
) where E : Throwable,
      E : NetworkErrorPredicate {

    /**
     * Resolves the cache operation if present.
     */
    fun getResolvedOperation(call: Call<Any>): ResolvedOperation<R>? {
        val requestMetadata = PlainRequestMetadata(
            responseClass,
            call.request().url().toString(),
            requestBodyConverter(call.request())
        )

        val operationToMethod: Pair<Operation, Method>? =
            getPredicateOperation(requestMetadata)?.let { it to PREDICATE }
                ?: getHeaderOperation(call)?.let { it to HEADER }
                ?: getAnnotationOperation()?.let { it to ANNOTATION }

        return if (operationToMethod != null) {
            val (operation, method) = operationToMethod

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

            ResolvedOperation(operation, method, requestMetadata)
        } else {
            logger.d(
                this,
                "No cache operation found for $methodDescription, the call will proceed without caching."
            )
            null
        }
    }

    private fun getPredicateOperation(requestMetadata: RequestMetadata<R>): Operation.Remote? {
        logger.d(this, "Checking cache predicate on $methodDescription")
        return operationPredicate(requestMetadata)
    }

    private fun getHeaderOperation(call: Call<Any>): Operation? {
        logger.d(this, "Checking cache header on $methodDescription")
        val header = call.request().header(DejaVuHeader) ?: return null

        val operation = try {
            header.toOperation()
        } catch (e: Exception) {
            null
        }

        if (operation == null) {
            logger.e(
                this,
                "Found a header cache operation on $methodDescription but it could not be deserialised: $header"
            )
        }

        return operation
    }

    private fun getAnnotationOperation(): Operation? {
        logger.d(this, "Checking the call's annotations on $methodDescription")
        return annotationOperation
    }

    internal enum class Method {
        PREDICATE,
        HEADER,
        ANNOTATION
    }

    data class ResolvedOperation<R>(
        val operation: Operation,
        val method: Method,
        val requestMetadata: PlainRequestMetadata<R>
    )

    internal class Factory<E>(
        private val operationPredicate: (RequestMetadata<*>) -> Operation.Remote?,
        private val requestBodyConverter: (Request) -> String?,
        private val logger: Logger
    ) where E : Throwable,
          E : NetworkErrorPredicate {

        fun <R> create(
            methodDescription: String,
            responseClass: Class<R>,
            annotationOperation: Operation?
        ) = RetrofitOperationResolver<E, R>(
            responseClass,
            requestBodyConverter,
            operationPredicate,
            annotationOperation,
            methodDescription,
            logger
        )
    }
}
