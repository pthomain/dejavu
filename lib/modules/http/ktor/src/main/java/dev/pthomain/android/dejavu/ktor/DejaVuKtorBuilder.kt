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
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate

/**
 * Builder for creating DejaVuKtor instances.
 *
 * Usage:
 * ```
 * val dejaVuKtor = DejaVuKtorBuilder<MyError>()
 *     .withInterceptorFactory(dejaVu.interceptorFactory)
 *     .build()
 * ```
 *
 * @param E the error type (must extend both Throwable and NetworkErrorPredicate)
 */
class DejaVuKtorBuilder<E>
    where E : Throwable,
          E : NetworkErrorPredicate {

    private var interceptorFactory: DejaVuInterceptor.Factory<E>? = null

    /**
     * Sets the DejaVuInterceptor.Factory to use for caching.
     *
     * @param factory the interceptor factory from a DejaVu instance
     */
    fun withInterceptorFactory(factory: DejaVuInterceptor.Factory<E>) = apply {
        this.interceptorFactory = factory
    }

    /**
     * Builds a DejaVuKtor instance.
     *
     * @throws IllegalStateException if interceptorFactory has not been set
     */
    fun build(): DejaVuKtor<E> {
        val factory = interceptorFactory
            ?: throw IllegalStateException(
                "interceptorFactory must be set via withInterceptorFactory()"
            )

        return DejaVuKtor(factory)
    }
}
