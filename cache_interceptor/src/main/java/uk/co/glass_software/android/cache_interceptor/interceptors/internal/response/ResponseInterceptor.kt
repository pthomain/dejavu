/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.cache_interceptor.interceptors.internal.response

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.SingleTransformer
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.CacheException

/**
 * Intercepts the response wrapper returned from the error and cache interceptors and returns the actual
 * response while adding the metadata if possible.
 * Only response classes implementing CacheMetadata.Holder will receive metadata and
 * can be used with the mergeOnNextOnError directive, otherwise an exception is thrown.
 *
 * @param logger the logger
 * @param start the time the call started
 * @param mergeOnNextOnError whether or not any exception should be added to the metadata on an empty response and delivered via onNext. This is only applied if the response implements CacheMetadata.Holder. An exception is thrown otherwise.
 */
internal class ResponseInterceptor<E>(private val logger: Logger,
                                      private val start: Long,
                                      private val mergeOnNextOnError: Boolean)
    : ObservableTransformer<ResponseWrapper<E>, Any>,
        SingleTransformer<ResponseWrapper<E>, Any>
        where E : Exception,
              E : NetworkErrorProvider {

    /**
     * Composes an Observable call.
     *
     * @param upstream the Observable to compose
     * @return the composed Observable
     */
    override fun apply(upstream: Observable<ResponseWrapper<E>>) =
            upstream.flatMap(this::intercept)

    /**
     * Composes an Single call.
     *
     * @param upstream the Single to compose
     * @return the composed Single
     */
    override fun apply(upstream: Single<ResponseWrapper<E>>) =
            upstream
                    .toObservable()
                    .compose(this)
                    .firstOrError()!!

    /**
     * Converts the ResponseWrapper into the expected response with added cache metadata if possible.
     *
     * @param wrapper the response wrapper returned by the error and cache interceptors
     *
     * @return an Observable emitting the expected response with associated metadata or an error if the empty response could not be created.
     */
    private fun intercept(wrapper: ResponseWrapper<E>): Observable<Any> {
        val responseClass = wrapper.responseClass
        val metadata = wrapper.metadata
        val operation = metadata.cacheToken.instruction.operation

        val mergeOnNextOnError = (operation as? Expiring)?.mergeOnNextOnError
                ?: this.mergeOnNextOnError

        val response = wrapper.response ?: createEmptyResponse(mergeOnNextOnError, responseClass)

        return if (response == null) {
            checkForError(
                    responseClass,
                    operation,
                    mergeOnNextOnError
            )
                    ?: metadata.exception
                    ?: IllegalStateException("Something went wrong")
        } else {
            addMetadataIfPossible(
                    response,
                    responseClass,
                    metadata,
                    operation,
                    mergeOnNextOnError
            ) ?: response
        }.let {
            if (it is Throwable) {
                logger.d("Returning error: $it")
                Observable.error<Any>(it)
            } else {
                logger.d("Returning response: $metadata")
                Observable.just(response)
            }
        }
    }

    /**
     * Creates an empty response to be returned in lieu of an exception if the mergeOnNextOnError
     * is set to true and the response class implements CacheMetadata.Holder.
     *
     * @param mergeOnNextOnError whether or not any exception should be added to the metadata on an empty response and delivered via onNext. This is only applied if the response implements CacheMetadata.Holder. An exception is thrown otherwise.
     * @param responseClass the target response class
     *
     * @return the empty response if possible
     */
    private fun createEmptyResponse(mergeOnNextOnError: Boolean,
                                    responseClass: Class<*>) =
            if (mergeOnNextOnError) {
                try {
                    responseClass.newInstance()
                } catch (e: Exception) {
                    null
                }
            } else null

    /**
     * Adds metadata to any response implementing CacheMetadata.Holder, or throws an exception if the
     * mergeOnNextOnError directive is used but the response does not implement CacheMetadata.Holder.
     */
    @Suppress("UNCHECKED_CAST")
    private fun addMetadataIfPossible(response: Any,
                                      responseClass: Class<*>,
                                      metadata: CacheMetadata<E>,
                                      operation: CacheInstruction.Operation?,
                                      mergeOnNextOnError: Boolean): CacheException? {
        val holder = response as? CacheMetadata.Holder<E>
        return if (holder == null) {
            checkForError(responseClass, operation, mergeOnNextOnError)
        } else {
            holder.metadata = metadata.copy(
                    callDuration = metadata.callDuration.copy(
                            total = (System.currentTimeMillis() - start).toInt()
                    )
            )
            null
        }
    }

    /**
     * Returns an exception if the mergeOnNextOnError directive is set to true but the
     * response class does not implement CacheMetadata.Holder.
     *
     * @param responseClass the target response class
     * @param operation the cache operation
     * @param mergeOnNextOnError whether or not any exception should be added to the metadata on an empty response and delivered via onNext. This is only applied if the response implements CacheMetadata.Holder. An exception is thrown otherwise.
     */
    private fun checkForError(responseClass: Class<*>,
                              operation: CacheInstruction.Operation?,
                              mergeOnNextOnError: Boolean): CacheException? {
        val message = "Could not add cache metadata to response '${responseClass.simpleName}'." +
                " If you want to enable metadata for this class, it needs extend the" +
                " 'CacheMetadata.Holder' interface." +
                " The 'mergeOnNextOnError' directive will be cause an exception to be thrown for classes" +
                " that do not support cache metadata."

        return if (operation is Expiring && mergeOnNextOnError) {
            CacheException(CacheException.Type.METADATA, message)
        } else {
            logger.d(message)
            null
        }
    }
}
