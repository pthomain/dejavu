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

package dev.pthomain.android.dejavu.ktor

import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.PlainRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.error.ErrorFactory
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.error.Outcome
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Entry point for integrating DejaVu caching with Ktor HTTP client.
 *
 * Provides the [DejaVuPlugin] installation and access to the underlying
 * [DejaVuInterceptor.Factory] for advanced use cases, including the
 * [cachedFlow] extension for stale-then-fresh patterns.
 *
 * Usage:
 * ```
 * val dejaVuKtor = DejaVuKtor(dejaVu.interceptorFactory, errorFactory)
 *
 * val client = HttpClient {
 *     dejaVuKtor.install(this)
 * }
 * ```
 *
 * @param E the error type (must extend both Throwable and NetworkErrorPredicate)
 * @param interceptorFactory the DejaVuInterceptor.Factory from the core DejaVu instance
 * @param errorFactory the ErrorFactory for converting exceptions to the error type E
 */
class DejaVuKtor<E>(
    val interceptorFactory: DejaVuInterceptor.Factory<E>,
    val errorFactory: ErrorFactory<E>
) where E : Throwable,
        E : NetworkErrorPredicate {

    /**
     * Installs the DejaVu caching plugin into a Ktor HttpClient configuration.
     *
     * @param clientConfig the Ktor HttpClientConfig to install into
     */
    fun install(clientConfig: HttpClientConfig<*>) {
        clientConfig.install(DejaVuPlugin) {
            interceptorFactory = this@DejaVuKtor.interceptorFactory
            errorFactory = this@DejaVuKtor.errorFactory
        }
    }

    companion object {
        /**
         * Creates a new DejaVuKtor instance.
         *
         * @param interceptorFactory the DejaVuInterceptor.Factory from a DejaVu instance
         * @param errorFactory the ErrorFactory for converting exceptions
         * @return a new DejaVuKtor instance configured with the given factory
         */
        fun <E> create(
            interceptorFactory: DejaVuInterceptor.Factory<E>,
            errorFactory: ErrorFactory<E>
        ): DejaVuKtor<E> where E : Throwable, E : NetworkErrorPredicate =
            DejaVuKtor(interceptorFactory, errorFactory)
    }
}

/**
 * Creates a [Flow] that emits [DejaVuResult] items from a cached Ktor request.
 *
 * This is the recommended way to use DejaVu with Ktor for stale-then-fresh patterns.
 * The returned Flow may emit multiple items: first a cached (stale) result if available,
 * then a fresh result from the network.
 *
 * Usage:
 * ```
 * dejaVuKtor.cachedFlow<MyResponse>(client, "https://api.example.com/data")
 *     .collect { result ->
 *         when (result) {
 *             is Response -> handleResponse(result.response)
 *             is Empty -> handleError(result.exception)
 *         }
 *     }
 * ```
 *
 * @param T the response type
 * @param client the Ktor [HttpClient] to use for network requests
 * @param url the URL to request
 * @param operation the cache operation to apply (defaults to [Operation.Remote.Cache])
 * @param configure optional [HttpRequestBuilder] configuration block
 * @return a [Flow] of [DejaVuResult] items
 */
inline fun <reified T : Any> DejaVuKtor<*>.cachedFlow(
    client: HttpClient,
    url: String,
    operation: Operation.Remote.Cache = Operation.Remote.Cache(),
    crossinline configure: HttpRequestBuilder.() -> Unit = {}
): Flow<DejaVuResult<T>> {
    val requestMetadata = PlainRequestMetadata(
        responseClass = T::class.java,
        url = url,
        requestBody = null
    )

    @Suppress("UNCHECKED_CAST")
    val typedInterceptorFactory = interceptorFactory
        as DejaVuInterceptor.Factory<Nothing>

    val interceptor = typedInterceptorFactory.create(
        asResult = true,
        operation = operation,
        requestMetadata = requestMetadata as PlainRequestMetadata<Any>
    )

    // Create an upstream Flow that performs the actual Ktor network call.
    // The result is wrapped in an Outcome so DejaVu's interceptor chain can
    // distinguish between successful and failed network responses.
    val upstream: Flow<Any> = flow {
        try {
            val response = client.get(url) { configure() }
            val body = response.body<T>()
            emit(Outcome.Success(body) as Any)
        } catch (e: Exception) {
            emit(Outcome.Error(errorFactory(e)) as Any)
        }
    }

    // Apply DejaVu's interceptor chain, which handles cache lookup, storage,
    // and may emit both cached (stale) and fresh results.
    @Suppress("UNCHECKED_CAST")
    return interceptor.intercept(upstream)
        .map { it as DejaVuResult<T> }
}
