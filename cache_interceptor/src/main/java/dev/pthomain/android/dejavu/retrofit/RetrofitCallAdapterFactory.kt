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

package dev.pthomain.android.dejavu.retrofit

import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.OperationSerialiser
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.response.DejaVuResult
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.annotations.CacheException
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.Request
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

/**
 * Implements the call adapter factory for Retrofit composing the calls with DejaVuInterceptor.
 *
 * @param configuration the global cache configuration
 * @param rxJava2CallAdapterFactory the default RxJava call adapter factory
 * @param dateFactory provides a date for a given timestamp or the current date with no argument
 * @param dejaVuFactory the DejaVuInterceptor factory
 * @param requestBodyConverter a factory converting a Request to String
 * @param annotationProcessor the Retrofit annotation processor
 * @param logger the logger
 */
class RetrofitCallAdapterFactory<E> internal constructor(private val configuration: DejaVuConfiguration<E>,
                                                         private val rxJava2CallAdapterFactory: RxJava2CallAdapterFactory,
                                                         private val innerFactory: (DejaVuInterceptor.Factory<E>, String, Class<*>, Boolean, Operation?, CallAdapter<Any, Any>) -> CallAdapter<*, *>,
                                                         private val dateFactory: (Long?) -> Date,
                                                         private val dejaVuFactory: DejaVuInterceptor.Factory<E>,
                                                         private val requestBodyConverter: (Request) -> String?,
                                                         private val annotationProcessor: AnnotationProcessor<E>,
                                                         private val logger: Logger)
    : CallAdapter.Factory()
        where E : Exception,
              E : NetworkErrorPredicate {

    /**
     * Returns a call adapter for interface methods that return {@code returnType}, or null if it
     * cannot be handled by this factory.
     *
     * @param returnType the call's return type
     * @param annotations the call's annotations
     * @param retrofit Retrofit instance
     *
     * @return the Retrofit call adapter handling the cache
     */
    override fun get(returnType: Type,
                     annotations: Array<Annotation>,
                     retrofit: Retrofit): CallAdapter<*, *> {
        val rawType = getRawType(returnType)

        return if (rawType == null) {
            logger.d(
                    this,
                    "Call with return type $returnType is not supported by DejaVu, using default RxJava adapter"
            )
            getDefaultCallAdapter(
                    returnType,
                    annotations,
                    retrofit
            )
        } else {
            val (unwrappedResponseType, responseClass) = unwrap(
                    getFirstParameterUpperBound(returnType)!!
            )

            val defaultCallAdapter = getDefaultCallAdapter(
                    unwrappedResponseType,
                    annotations,
                    retrofit
            )

            val methodDescription = "call returning " + getTypedName(
                    responseClass,
                    unwrappedResponseType != responseClass,
                    rawType == Single::class.java
            )

            logger.d(
                    this,
                    "Processing annotation for $methodDescription"
            )

            val operation = try {
                annotationProcessor.process(
                        annotations,
                        responseClass
                )
            } catch (cacheException: CacheException) {
                logger.e(
                        this,
                        cacheException,
                        "The annotation on $methodDescription cannot be processed, defaulting to other cache methods if available"
                )
                return defaultCallAdapter
            }

            if (operation == null) {
                logger.d(
                        this,
                        "Annotation processor for $methodDescription"
                                + " returned no instruction, defaulting to other cache methods if available"
                )
            } else {
                logger.d(
                        this,
                        "Annotation processor for $methodDescription"
                                + " returned the following cache operation "
                                + operation
                )
            }

            val isWrapped = responseClass != unwrappedResponseType

            innerFactory(
                    dejaVuFactory,
                    methodDescription,
                    responseClass,
                    isWrapped,
                    operation,
                    defaultCallAdapter
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getDefaultCallAdapter(responseType: Type,
                                      annotations: Array<Annotation>,
                                      retrofit: Retrofit) =
            rxJava2CallAdapterFactory.get(
                    responseType,
                    annotations,
                    retrofit
            ) as CallAdapter<Any, Any>

    private fun unwrap(responseType: Type) =
            if (responseType == DejaVuResult::class.java) {
                val responseClass = getFirstParameterUpperBound(responseType)!!

                val unwrappedType = object : ParameterizedType {
                    override fun getRawType() = Observable::class.java
                    override fun getOwnerType() = null
                    override fun getActualTypeArguments(): Array<Class<out Any>?> {
                        return arrayOf(responseClass)
                    }
                }

                unwrappedType to responseClass
            } else responseType to responseType as Class<*>

    private fun getFirstParameterUpperBound(returnType: Type) =
            if (returnType is ParameterizedType)
                getRawType(getParameterUpperBound(0, returnType))
            else null

    private fun getTypedName(responseClass: Class<*>,
                             isWrapped: Boolean,
                             isSingle: Boolean) =
            String.format(
                    ifElse(isSingle, "Single<%s>", "Observable<%s>"),
                    String.format(
                            ifElse(isWrapped, "DejaVuResult<%s>", "%s"),
                            responseClass.simpleName
                    )
            )
}
