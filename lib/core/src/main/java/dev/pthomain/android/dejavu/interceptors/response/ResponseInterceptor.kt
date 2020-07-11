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
import dev.pthomain.android.boilerplate.core.utils.rx.observable
import dev.pthomain.android.dejavu.cache.metadata.response.*
import dev.pthomain.android.dejavu.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.DoNotCache
import dev.pthomain.android.dejavu.di.DateFactory
import dev.pthomain.android.dejavu.di.ellapsed
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import io.reactivex.Observable
import io.reactivex.ObservableTransformer

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
 * @param emptyResponseFactory factory providing empty response wrappers
 * @param configuration the cache configuration
 */
internal class ResponseInterceptor<R : Any, E> private constructor(
        private val logger: Logger,
        private val dateFactory: DateFactory,
        private val emptyResponseFactory: EmptyResponseFactory<E>,
        private val asResult: Boolean,
) : ObservableTransformer<DejaVuResult<R>, Any>
        where E : Throwable,
              E : NetworkErrorPredicate {

    /**
     * Composes an Observable call.
     *
     * @param upstream the Observable to compose
     * @return the composed Observable
     */
    override fun apply(upstream: Observable<DejaVuResult<R>>) =
            upstream.flatMap(::intercept)!!

    /**
     * Converts the ResponseWrapper into the expected response with added cache metadata if possible.
     *
     * @param wrapper the response wrapper returned by the error and cache interceptors
     *
     * @return an Observable emitting the expected response with associated metadata or an error if the empty response could not be created.
     */
    @Suppress("UNCHECKED_CAST")
    private fun intercept(wrapper: DejaVuResult<R>): Observable<Any> {
        with(wrapper.hasMetadata) {
            callDuration = callDuration.copy(
                    total = (dateFactory(null).time - cacheToken.requestDate.time).toInt()
            )
        }

        if (wrapper is Response<*, *> && wrapper.response is HasMetadata<*, *, *>) {
            when (wrapper.cacheToken.instruction.operation) {
                is Cache -> setMetadata(wrapper)
                DoNotCache -> setMetadata(wrapper)
            }
        }

        fun <R> addDuration(response: R) = response.apply {
            if (this is HasMetadata<*, *, *>) {
                callDuration = callDuration.copy(
                        total = wrapper.hasMetadata.cacheToken.requestDate.ellapsed(dateFactory)
                )
            }
        } as Any

        return if (asResult) wrapper.observable()
        else when (wrapper) {
            is Response<R, *> -> wrapper.response.observable()
            is Empty<R, *, *> -> Observable.error(wrapper.exception)
            is Result<R, *> -> Observable.error(NoSuchElementException("This operation does not return any response")) //TODO check this
        }.map(::addDuration) as Observable<Any>
    }

    @Suppress("UNCHECKED_CAST")
    private fun <R : Any, O : Remote> setMetadata(wrapper: Response<R, O>) {
        (wrapper.response as HasMetadata<R, O, ResponseToken<O, R>>).apply {
            cacheToken = wrapper.cacheToken
            callDuration = wrapper.callDuration
        }
    }

    internal class Factory<E>(
            private val logger: Logger,
            private val dateFactory: DateFactory,
            private val emptyResponseFactory: EmptyResponseFactory<E>,
    ) where E : Throwable,
            E : NetworkErrorPredicate {

        fun <R : Any> create(asResult: Boolean) = ResponseInterceptor<R, E>(
                logger,
                dateFactory,
                emptyResponseFactory,
                asResult
        )
    }
}
