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

package dev.pthomain.android.dejavu.interceptors.response

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.metadata.response.*
import dev.pthomain.android.dejavu.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.DoNotCache
import dev.pthomain.android.dejavu.di.DateFactory
import dev.pthomain.android.dejavu.di.ellapsed
import dev.pthomain.android.glitchy.core.interceptor.interceptors.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.flow.interceptors.base.FlowInterceptor

/**
 * Intercepts the response wrapper returned from the error and cache interceptors and returns the actual
 * response while adding the metadata if possible. Only response classes implementing CacheMetadata.Holder
 * will receive metadata and can be used with the mergeOnNextOnError directive, otherwise an error is emitted.
 *
 * This class also deals with filtering the returned responses according to the
 * filterFinal/freshOnly/allowNonFinalForSingle directives set for this call globally or specifically.
 *
 * @param logger the logger
 * @param dateFactory provides a date for a given timestamp or the current date with no argument
 * @param configuration the cache configuration
 */
internal class ResponseInterceptor<R : Any, E> private constructor(
        private val logger: Logger,
        private val dateFactory: DateFactory,
        private val asResult: Boolean,
) : FlowInterceptor()
        where E : Throwable,
              E : NetworkErrorPredicate {

    /**
     * Converts the ResponseWrapper into the expected response with added cache metadata if possible.
     *
     * @param wrapper the response wrapper returned by the error and cache interceptors
     *
     * @return an Observable emitting the expected response with associated metadata or an error if the empty response could not be created.
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun map(value: Any): Any {
        val wrapper = value as DejaVuResult<R>

        fun <O : Remote> Response<R, O>.updateResponseMetadata() = response.apply {
            if (this is HasMetadata<*, *, *>) {
                this as HasMetadata<R, O, ResponseToken<O, R>>
                cacheToken = wrapper.hasMetadata.cacheToken as ResponseToken<O, R>
                callDuration = wrapper.hasMetadata.callDuration.copy(
                        total = wrapper.hasMetadata.cacheToken.requestDate.ellapsed(dateFactory)
                )
            }
        }

        return if (asResult) wrapper
        else when (wrapper) {
            is Response<R, *> -> when (wrapper.cacheToken.instruction.operation) {
                is Cache -> wrapper.updateResponseMetadata()
                DoNotCache -> wrapper.updateResponseMetadata()
            }

            is Empty<R, *, *> -> throw wrapper.exception
            is Result<R, *> -> throw NoSuchElementException("This operation does not return any response")
        }
    }

    internal class Factory<E>(
            private val logger: Logger,
            private val dateFactory: DateFactory
    ) where E : Throwable,
            E : NetworkErrorPredicate {

        fun <R : Any> create(asResult: Boolean) = ResponseInterceptor<R, E>(
                logger,
                dateFactory,
                asResult
        )
    }
}
