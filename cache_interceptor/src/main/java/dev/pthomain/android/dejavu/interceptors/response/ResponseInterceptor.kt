/*
 *
 *  Copyright (C) 2017 Pierre Thomain
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

import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.boilerplate.core.utils.rx.observable
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.interceptors.RxType
import dev.pthomain.android.dejavu.interceptors.RxType.OPERATION
import dev.pthomain.android.dejavu.interceptors.RxType.SINGLE
import dev.pthomain.android.dejavu.interceptors.cache.instruction.DejaVuCall
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation.Cache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseFactory.DoneException
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseFactory.EmptyResponseException
import dev.pthomain.android.dejavu.utils.Utils.isAnyInstance
import dev.pthomain.android.dejavu.utils.Utils.swapLambdaWhen
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.Predicate
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
 * @param instructionToken the instruction cache token
 * @param rxType the returned RxJava type
 * @param start the time the call started
 */
internal class ResponseInterceptor<E>(private val logger: Logger,
                                      private val dateFactory: (Long?) -> Date,
                                      private val emptyResponseFactory: EmptyResponseFactory<E>,
                                      private val configuration: DejaVuConfiguration<E>,
                                      private val metadataSubject: PublishSubject<CacheMetadata<E>>,
                                      private val instructionToken: CacheToken,
                                      private val rxType: RxType,
                                      private val start: Long)
    : ObservableTransformer<ResponseWrapper<E>, Any>
        where E : Exception,
              E : NetworkErrorPredicate {

    private val responseFilter = Predicate<ResponseWrapper<E>> {
        val status = it.metadata.cacheToken.status
        val operation = instructionToken.instruction.operation

        ifElse(
                operation is Cache,
                ifElse(
                        rxType == SINGLE,
                        status.isFinal,
                        true
                ),
                true
        )
    }

    /**
     * Composes an Observable call.
     *
     * @param upstream the Observable to compose
     * @return the composed Observable
     */
    override fun apply(upstream: Observable<ResponseWrapper<E>>) =
            upstream.filter(responseFilter)
                    .switchIfEmpty(Observable.defer {
                        emptyResponseFactory.emptyResponseWrapper(instructionToken).observable() //TODO this should not happen
                    })
                    .flatMap(this::intercept)!!

    /**
     * Converts the ResponseWrapper into the expected response with added cache metadata if possible.
     *
     * @param wrapper the response wrapper returned by the error and cache interceptors
     *
     * @return an Observable emitting the expected response with associated metadata or an error if the empty response could not be created.
     */
    private fun intercept(wrapper:ResponseWrapper<E>): Observable<Any> {
        val errorFactory = configuration.errorFactory

        val exception = wrapper.metadata.exception.swapLambdaWhen({ it == null }) {
            errorFactory(IllegalStateException("The response is null but no exception is present in the metadata"))
        }

        val callDuration = wrapper.metadata.callDuration.copy(
                total = (dateFactory(null).time - start).toInt()
        )

        val metadata = wrapper.metadata.copy(
                exception = exception,
                callDuration = callDuration
        )

        val response = wrapper.response

        metadataSubject.onNext(metadata)

        if (response is CacheMetadata.Holder<*>
                && response.metadata.exceptionClass.isAssignableFrom(errorFactory.exceptionClass)) {
            @Suppress("UNCHECKED_CAST") // This is verified by the above check
            (response as CacheMetadata.Holder<E>).metadata = metadata
        }

        val isEmptyException = exception?.cause.isAnyInstance(
                EmptyResponseException::class.java,
                DoneException::class.java
        )

        return when {
            rxType == OPERATION -> {
                @Suppress("UNCHECKED_CAST")
                DejaVuCall.Resolved<Any, E>(
                        wrapper.observable(),
                        errorFactory.exceptionClass
                ) as Observable<Any>
            }

            exception != null -> {
                logger.d(this, "Returning error: $exception")
                Observable.error<Any>(exception)
            }

            else -> {
                logger.d(this, "Returning response: $metadata")
                Observable.just<Any>(response)
            }
        }
    }

}
