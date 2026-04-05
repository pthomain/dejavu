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
import io.ktor.client.plugins.api.createClientPlugin

/**
 * Configuration for the DejaVu Ktor client plugin.
 *
 * @property interceptorFactory the DejaVuInterceptor.Factory used to create interceptors
 *     for caching. Must be set before the plugin is installed.
 */
class DejaVuPluginConfig {
    var interceptorFactory: DejaVuInterceptor.Factory<*>? = null
}

/**
 * Ktor client plugin that integrates DejaVu caching into the HTTP pipeline.
 *
 * This plugin intercepts Ktor HTTP requests that carry a [DejaVuCacheAttribute]
 * and applies the DejaVu caching interceptor chain. Requests without cache
 * attributes pass through unmodified.
 *
 * Usage:
 * ```
 * val client = HttpClient {
 *     install(DejaVuPlugin) {
 *         interceptorFactory = dejaVu.interceptorFactory
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
 * The plugin currently intercepts at the Send phase and checks for cache
 * attributes on each request. Full cache integration (serving from cache,
 * storing responses) requires the DejaVuInterceptor chain to be wired through
 * the response pipeline.
 *
 * TODO: Implement full response body interception for cache storage and retrieval.
 * The current implementation passes through to the network and marks requests
 * for cache processing. A complete implementation would:
 * 1. Check the cache before making a network request
 * 2. Serve cached responses when appropriate (based on priority)
 * 3. Store network responses in the cache
 * 4. Emit both cached and fresh responses for STALE_ACCEPTED_FIRST priority
 */
val DejaVuPlugin = createClientPlugin("DejaVu", ::DejaVuPluginConfig) {
    val interceptorFactory = pluginConfig.interceptorFactory
        ?: throw IllegalStateException(
            "DejaVuPlugin requires an interceptorFactory. " +
                "Set it via: install(DejaVuPlugin) { interceptorFactory = ... }"
        )

    onRequest { request, _ ->
        val cacheOperation = request.attributes.getOrNull(DejaVuCacheAttribute)
        if (cacheOperation != null) {
            // The cache operation is attached to the request attributes.
            // It will be read by the response handling phase to apply caching.
            // For now, we just ensure the attribute is propagated.
        }
    }

    // TODO: Implement transformResponseBody to intercept and cache responses.
    // This requires converting the Ktor response pipeline to work with
    // DejaVuInterceptor's Observable-based API, or adapting it to work
    // with suspend functions directly.
}
