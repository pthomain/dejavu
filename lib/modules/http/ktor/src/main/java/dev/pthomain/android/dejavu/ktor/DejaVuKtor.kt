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

import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import io.ktor.client.HttpClientConfig

/**
 * Entry point for integrating DejaVu caching with Ktor HTTP client.
 *
 * Provides the [DejaVuPlugin] installation and access to the underlying
 * [DejaVuInterceptor.Factory] for advanced use cases.
 *
 * Usage:
 * ```
 * val dejaVuKtor = DejaVuKtor(dejaVu.interceptorFactory)
 *
 * val client = HttpClient {
 *     dejaVuKtor.install(this)
 * }
 * ```
 *
 * @param E the error type (must extend both Throwable and NetworkErrorPredicate)
 * @param interceptorFactory the DejaVuInterceptor.Factory from the core DejaVu instance
 */
class DejaVuKtor<E> internal constructor(
    val interceptorFactory: DejaVuInterceptor.Factory<E>
) where E : Throwable, E : NetworkErrorPredicate {

    /**
     * Installs the DejaVu caching plugin into a Ktor HttpClient configuration.
     *
     * @param clientConfig the Ktor HttpClientConfig to install into
     */
    fun install(clientConfig: HttpClientConfig<*>) {
        clientConfig.install(DejaVuPlugin) {
            interceptorFactory = this@DejaVuKtor.interceptorFactory
        }
    }

    companion object {
        /**
         * Creates a new DejaVuKtor instance.
         *
         * @param interceptorFactory the DejaVuInterceptor.Factory from a DejaVu instance
         * @return a new DejaVuKtor instance configured with the given factory
         */
        fun <E> create(
            interceptorFactory: DejaVuInterceptor.Factory<E>
        ): DejaVuKtor<E> where E : Throwable, E : NetworkErrorPredicate =
            DejaVuKtor(interceptorFactory)
    }
}
