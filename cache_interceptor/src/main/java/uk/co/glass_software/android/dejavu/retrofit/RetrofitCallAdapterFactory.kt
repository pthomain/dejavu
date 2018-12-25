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

package uk.co.glass_software.android.dejavu.retrofit

import com.google.gson.internal.`$Gson$Types`.getRawType
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.DoNotCache
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.DejaVuInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.*
import uk.co.glass_software.android.dejavu.retrofit.annotations.CacheException
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
                                                         private val dateFactory: (Long?) -> Date,
                                                         private val dejaVuFactory: DejaVuInterceptor.Factory<E>,
                                                         private val annotationProcessor: AnnotationProcessor<E>,
                                                         private val processingErrorAdapterFactory: ProcessingErrorAdapter.Factory<E>,
                                                         private val logger: Logger)
    : CallAdapter.Factory()
        where E : Exception,
              E : NetworkErrorProvider {

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
            Completable::class.java -> COMPLETABLE
            else -> null
        }?.let { rxType ->
            val responseClass = when (rxType) {
                OBSERVABLE,
                SINGLE -> {
                    if (returnType is ParameterizedType)
                        getRawType(getParameterUpperBound(0, returnType))
                    else null
                }
                else -> null
            } ?: Any::class.java

            logger.d(
                    this,
                    "Processing annotation for method returning " + rxType.getTypedName(responseClass)
            )

            try {
                annotationProcessor.process(
                        annotations,
                        rxType,
                        responseClass
                ).let { instruction ->
                    val methodDescription = "method returning " + rxType.getTypedName(responseClass)

                    if (instruction == null) {
                        logger.d(
                                this,
                                "Annotation processor for $methodDescription"
                                        + " returned no instruction, checking cache header"
                        )
                    } else {
                        logger.d(
                                this,
                                "Annotation processor for $methodDescription"
                                        + " returned the following instruction "
                                        + instruction
                        )
                    }

                    RetrofitCallAdapter(
                            dejaVuFactory,
                            logger,
                            methodDescription,
                            instruction,
                            defaultCallAdapter
                    )
                }
            } catch (cacheException: CacheException) {
                processingErrorAdapterFactory.create(
                        defaultCallAdapter,
                        CacheToken.fromInstruction(
                                CacheInstruction(responseClass, DoNotCache),
                                false,
                                false,
                                "",
                                null
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