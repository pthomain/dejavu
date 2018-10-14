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

package uk.co.glass_software.android.cache_interceptor.retrofit

import com.google.gson.internal.`$Gson$Types`.getRawType
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.AnnotationProcessor
import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.AnnotationProcessor.RxType.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

@Suppress("UNCHECKED_CAST")
class RetrofitCacheAdapterFactory<E> internal constructor(private val rxJava2CallAdapterFactory: RxJava2CallAdapterFactory,
                                                          private val rxCacheFactory: RxCacheInterceptor.Factory<E>,
                                                          private val annotationProcessor: AnnotationProcessor<E>,
                                                          private val logger: Logger)
    : CallAdapter.Factory()
        where E : Exception,
              E : NetworkErrorProvider {

    override fun get(returnType: Type,
                     annotations: Array<Annotation>,
                     retrofit: Retrofit): CallAdapter<*, *> {
        val callAdapter = rxJava2CallAdapterFactory.get(
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

            logger.d("Processing annotation for method returning " + rxType.getTypedName(responseClass))

            annotationProcessor.process(
                    annotations,
                    rxType,
                    responseClass
            ).let { instruction ->
                val methodDescription = "method returning " + rxType.getTypedName(responseClass)

                if (instruction == null) {
                    logger.d("Annotation processor for $methodDescription"
                            + " returned no instruction, checking cache header"
                    )
                } else {
                    logger.d("Annotation processor for $methodDescription"
                            + " returned the following instruction "
                            + instruction
                    )
                }
                RetrofitCacheAdapter(
                        rxCacheFactory,
                        logger,
                        methodDescription,
                        instruction,
                        callAdapter
                )
            }
        } ?: callAdapter.also {
            logger.d("Annotation processor did not return any instruction for call returning $returnType")
        }
    }

}