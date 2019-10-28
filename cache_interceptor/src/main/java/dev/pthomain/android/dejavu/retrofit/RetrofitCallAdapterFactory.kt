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

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.configuration.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction
import dev.pthomain.android.dejavu.configuration.instruction.CacheOperation
import dev.pthomain.android.dejavu.configuration.instruction.Operation
import dev.pthomain.android.dejavu.configuration.instruction.Operation.DoNotCache
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.NOT_CACHED
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.*
import dev.pthomain.android.dejavu.retrofit.annotations.CacheException
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

/**
 * Implements the call adapter factory for Retrofit composing the calls with DejaVuInterceptor.
 *
 * @param rxJava2CallAdapterFactory the default RxJava call adapter factory
 * @param dateFactory provides a date for a given timestamp or the current date with no argument
 * @param dejaVuFactory the DejaVuInterceptor factory, as returned by DejaVu
 * @param annotationProcessor the Retrofit annotation processor
 * @param logger the logger
 */
class RetrofitCallAdapterFactory<E> internal constructor(private val rxJava2CallAdapterFactory: RxJava2CallAdapterFactory,
                                                         private val innerFactory: (DejaVuInterceptor.Factory<E>, Logger, String, Class<*>, Operation?, CallAdapter<Any, Any>) -> CallAdapter<*, *>,
                                                         private val dateFactory: (Long?) -> Date,
                                                         private val dejaVuFactory: DejaVuInterceptor.Factory<E>,
                                                         private val annotationProcessor: AnnotationProcessor<E>,
                                                         private val processingErrorAdapterFactory: ProcessingErrorAdapter.Factory<E>,
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
        @Suppress("UNCHECKED_CAST")
        val defaultCallAdapter = rxJava2CallAdapterFactory.get(
                returnType,
                annotations,
                retrofit
        ) as CallAdapter<Any, Any>

        return when (getRawType(returnType)) {
            Single::class.java -> SINGLE
            Observable::class.java -> OBSERVABLE
            CacheOperation::class.java -> OPERATION
            else -> null
        }?.let { rxType ->

            val responseClass = if (returnType is ParameterizedType)
                getRawType(getParameterUpperBound(0, returnType))
            else Any::class.java //TODO throw exception

            logger.d(
                    this,
                    "Processing annotation for method returning " + rxType.getTypedName(responseClass)
            )

            try {
                annotationProcessor.process(
                        annotations,
                        rxType,
                        responseClass
                ).let { operation ->
                    val methodDescription = "method returning " + rxType.getTypedName(responseClass)

                    if (operation == null) {
                        logger.d(
                                this,
                                "Annotation processor for $methodDescription"
                                        + " returned no instruction, checking cache header"
                        )
                    } else {
                        logger.d(
                                this,
                                "Annotation processor for $methodDescription"
                                        + " returned the following cache operation "
                                        + operation
                        )
                    }

                    innerFactory(
                            dejaVuFactory,
                            logger,
                            methodDescription,
                            responseClass,
                            operation,
                            defaultCallAdapter
                    )
                }
            } catch (cacheException: CacheException) {
                val requestMetadata = RequestMetadata.Hashed.Invalid(responseClass)
                processingErrorAdapterFactory.create(
                        defaultCallAdapter,
                        CacheToken(
                                CacheInstruction(
                                        requestMetadata,
                                        DoNotCache
                                ),
                                NOT_CACHED,
                                false,
                                false
                        ),
                        dateFactory(null).time,
                        rxType,
                        cacheException
                )
            }
        } ?: defaultCallAdapter.also {
            logger.d(
                    this,
                    "Annotation processor did not return any instruction for call returning $returnType"
            )
        }
    }

}