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
import dev.pthomain.android.dejavu.error.Outcome
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import io.ktor.client.plugins.api.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.lang.reflect.ParameterizedType

/**
 * Configuration for the DejaVu Ktor client plugin.
 *
 * @property interceptorFactory the DejaVuInterceptor.Factory used to create interceptors
 *     for caching. Must be set before the plugin is installed.
 * @property errorFactory the ErrorFactory used to convert exceptions to the appropriate
 *     error type. Must be set before the plugin is installed.
 */
class DejaVuPluginConfig {
    var interceptorFactory: DejaVuInterceptor.Factory<*>? = null
    var errorFactory: ErrorFactory<*>? = null
}

/**
 * Ktor client plugin that integrates DejaVu caching into the HTTP pipeline.
 *
 * This plugin intercepts Ktor HTTP requests that carry a [DejaVuCacheAttribute]
 * and applies the DejaVu caching interceptor chain. Requests without cache
 * attributes pass through unmodified.
 *
 * The plugin uses [transformResponseBody] to intercept responses when the caller
 * expects a [DejaVuResult] type. It wraps the already-fetched response body in
 * an [Outcome.Success], feeds it through the DejaVu interceptor chain (which
 * handles cache storage, staleness checks, and metadata decoration), and returns
 * the resulting [DejaVuResult].
 *
 * Usage:
 * ```
 * val client = HttpClient {
 *     install(DejaVuPlugin) {
 *         interceptorFactory = dejaVu.interceptorFactory
 *         errorFactory = myErrorFactory
 *     }
 * }
 *
 * // Then use cache DSL on requests:
 * client.get("/api/data") {
 *     cache(durationInSeconds = 300)
 *     responseClass<MyResponse>()
 * }
 * ```
 *
 * For stale-then-fresh patterns (where the Flow emits multiple values),
 * use [cachedFlow] from [DejaVuKtor] instead of the plugin's single-shot
 * response transformation.
 */
val DejaVuPlugin = createClientPlugin("DejaVu", ::DejaVuPluginConfig) {
    val interceptorFactory = pluginConfig.interceptorFactory
        ?: error(
            "DejaVuPlugin requires an interceptorFactory. " +
                "Set it via: install(DejaVuPlugin) { interceptorFactory = ... }"
        )
    val errorFactory = pluginConfig.errorFactory
        ?: error(
            "DejaVuPlugin requires an errorFactory. " +
                "Set it via: install(DejaVuPlugin) { errorFactory = ... }"
        )

    /**
     * Transform response bodies when the caller requests a [DejaVuResult] type.
     *
     * This hook fires after the network response has been received. When the request
     * was marked with cache attributes and the caller expects a [DejaVuResult], the
     * response body is routed through DejaVu's interceptor chain (which handles
     * caching, staleness checks, and metadata decoration).
     */
    transformResponseBody { response, body, requestedType ->
        val operation: Operation? = response.call.request.attributes.getOrNull(DejaVuCacheAttribute)

        if (operation != null && requestedType.type == DejaVuResult::class) {
            // Extract the inner type T from DejaVuResult<T>
            val javaType = requestedType.reifiedType
            val innerType = (javaType as? ParameterizedType)
                ?.actualTypeArguments?.firstOrNull()

            if (innerType != null) {
                val responseClass = (innerType as? Class<*>) ?: Any::class.java
                val url = response.call.request.url.toString()

                val requestMetadata = PlainRequestMetadata(
                    responseClass = responseClass,
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

                // Wrap the already-fetched body in an Outcome and create an upstream Flow
                val upstream = flowOf(Outcome.Success(body) as Any)

                // Apply DejaVu's interceptor chain and collect the first result.
                // Note: this only returns the first emission. For stale-then-fresh
                // patterns that emit multiple values, use cachedFlow() instead.
                interceptor.intercept(upstream).first()
            } else null
        } else null
    }
}
