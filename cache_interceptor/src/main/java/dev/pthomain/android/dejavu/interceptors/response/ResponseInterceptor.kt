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
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.subjects.PublishSubject
import java.util.*

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
 * @param metadataSubject the subject used to emit the current response's metadata (exposed as an Observable on DejaVu)
 */
//TODO check if this class is still needed
internal class ResponseInterceptor<R : Any, E> private constructor(
        private val logger: Logger,
        private val dateFactory: (Long?) -> Date,
        private val emptyResponseFactory: EmptyResponseFactory<E>,
        private val configuration: DejaVuConfiguration<E>,
        private val metadataSubject: PublishSubject<DejaVuResult<*>>
) : ObservableTransformer<DejaVuResult<R>, DejaVuResult<R>>
        where E : Throwable,
              E : NetworkErrorPredicate {

    /**
     * Composes an Observable call.
     *
     * @param upstream the Observable to compose
     * @return the composed Observable
     */
    override fun apply(upstream: Observable<DejaVuResult<R>>) =
            upstream.flatMap(this::intercept)!!

    /**
     * Converts the ResponseWrapper into the expected response with added cache metadata if possible.
     *
     * @param wrapper the response wrapper returned by the error and cache interceptors
     *
     * @return an Observable emitting the expected response with associated metadata or an error if the empty response could not be created.
     */
    @Suppress("UNCHECKED_CAST")
    private fun intercept(wrapper: DejaVuResult<R>): Observable<DejaVuResult<R>> {
        return wrapper.observable() //FIXME is this still needed?
//        val callDuration = wrapper.metadata.callDuration.copy(
//                total = (dateFactory(null).time - start).toInt()
//        )
//
//        val metadata = wrapper.metadata.copy(
//                exception = exception,
//                callDuration = callDuration
//        )
//
//        val response = wrapper.response
//
//        val result = convert(wrapper)
//        metadataSubject.onNext(result)
//
//        if (response is HasCacheToken<*, *, *>) {
//            (response as HasCacheToken<O, R, RequestToken<O, R>>).cacheToken = wrapper.metadata.cacheToken
//        }
//
//        if (response is HasCallDuration) {
//            response.callDuration = wrapper.metadata.callDuration
//        }
//
//        return when {
//            isWrapped -> result.observable() as Observable<Any>
//
//            exception != null -> {
//                logger.d(this, "Returning error: $exception")
//                Observable.error(exception)
//            }
//
//            else -> {
//                logger.d(this, "Returning response: $metadata")
//                Observable.just(response)
//            }
//        }
    }

//    //TODO JavaDoc
//    private fun convert(wrapper: ResponseWrapper<O, R, RequestToken<O, R>, E>): DejaVuResult<R> {
//        val metadata = wrapper.metadata
//        return if (wrapper.response == null) {
//            val exception = metadata.exception!!.cause
//
//            when (exception) {
//                is DoneException -> {
//                    val cacheToken = metadata.cacheToken.asRequest<Local>()
//                    Result(
//                            cacheToken,
//                            metadata.callDuration
//                    )
//                }
//                else -> Empty<R, Remote, E>(
//                        metadata.exception,
//                        metadata.cacheToken.asRequest(),
//                        metadata.callDuration
//                )
//            }
//        } else Response<R, Remote>(
//                wrapper.response,
//                metadata.cacheToken.asResponse(),
//                metadata.callDuration
//        )
//    }

    class Factory<E>(private val configuration: DejaVuConfiguration<E>,
                     private val logger: Logger,
                     private val dateFactory: (Long?) -> Date,
                     private val metadataSubject: PublishSubject<DejaVuResult<*>>,
                     private val emptyResponseFactory: EmptyResponseFactory<E>)
            where E : Throwable,
                  E : NetworkErrorPredicate {

        fun <R : Any> create() = ResponseInterceptor<R, E>(
                logger,
                dateFactory,
                emptyResponseFactory,
                configuration,
                metadataSubject
        )
    }
}
