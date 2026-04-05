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

import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.STALE_ACCEPTED_FIRST
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.util.AttributeKey

/**
 * Attribute key used to attach a DejaVu cache operation to a Ktor request.
 */
val DejaVuCacheAttribute = AttributeKey<Operation>("DejaVuCache")

/**
 * Attribute key for the response class type, needed for cache key generation.
 */
val DejaVuResponseClassAttribute = AttributeKey<Class<*>>("DejaVuResponseClass")

/**
 * DSL extension for setting a Cache operation on a Ktor request.
 *
 * Usage:
 * ```
 * client.get("/api/data") {
 *     cache(durationInSeconds = 300)
 * }
 * ```
 *
 * @param priority the cache priority strategy
 * @param durationInSeconds how long the cached data is considered fresh
 * @param connectivityTimeoutInSeconds max time to wait for connectivity (null = no timeout)
 * @param requestTimeOutInSeconds max time to wait for the request (null = no timeout)
 * @param serialisation serialisation configuration string
 */
fun HttpRequestBuilder.cache(
    priority: CachePriority = STALE_ACCEPTED_FIRST,
    durationInSeconds: Int = 3600,
    connectivityTimeoutInSeconds: Int? = null,
    requestTimeOutInSeconds: Int? = null,
    serialisation: String = ""
) {
    attributes.put(
        DejaVuCacheAttribute,
        Operation.Remote.Cache(
            priority = priority,
            durationInSeconds = durationInSeconds,
            serialisation = serialisation,
            connectivityTimeoutInSeconds = connectivityTimeoutInSeconds,
            requestTimeOutInSeconds = requestTimeOutInSeconds
        )
    )
}

/**
 * DSL extension to mark a Ktor request as not cacheable.
 */
fun HttpRequestBuilder.doNotCache() {
    attributes.put(DejaVuCacheAttribute, Operation.Remote.DoNotCache)
}

/**
 * DSL extension to invalidate the cache entry for a Ktor request.
 */
fun HttpRequestBuilder.invalidate() {
    attributes.put(DejaVuCacheAttribute, Operation.Local.Invalidate)
}

/**
 * DSL extension to clear cache entries for a Ktor request.
 *
 * @param scope the scope of entries to clear
 * @param clearStaleEntriesOnly whether to only clear stale entries
 */
fun HttpRequestBuilder.clearCache(
    scope: Operation.Local.Clear.Scope = Operation.Local.Clear.Scope.ALL,
    clearStaleEntriesOnly: Boolean = false
) {
    attributes.put(
        DejaVuCacheAttribute,
        Operation.Local.Clear(scope, clearStaleEntriesOnly)
    )
}

/**
 * DSL extension to set the expected response class for cache key generation.
 *
 * @param clazz the response class
 */
fun HttpRequestBuilder.responseClass(clazz: Class<*>) {
    attributes.put(DejaVuResponseClassAttribute, clazz)
}

/**
 * Inline reified version of responseClass for convenience.
 *
 * Usage:
 * ```
 * client.get("/api/data") {
 *     cache()
 *     responseClass<MyResponse>()
 * }
 * ```
 */
inline fun <reified T> HttpRequestBuilder.responseClass() {
    responseClass(T::class.java)
}
