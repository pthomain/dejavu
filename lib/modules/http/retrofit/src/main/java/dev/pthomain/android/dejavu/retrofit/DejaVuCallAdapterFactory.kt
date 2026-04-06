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

package dev.pthomain.android.dejavu.retrofit

import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.retrofit.annotations.processor.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.operation.RetrofitOperationResolver
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate
import kotlinx.coroutines.flow.Flow
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * A Retrofit CallAdapter.Factory that detects Flow<*> return types and
 * creates DejaVuCallAdapter instances to integrate the DejaVu cache interceptor chain.
 *
 * @param interceptorFactory factory for creating DejaVuInterceptor instances
 * @param annotationProcessor processes cache annotations on Retrofit methods
 * @param operationResolverFactory factory for creating RetrofitOperationResolver instances
 */
class DejaVuCallAdapterFactory<E> internal constructor(
        private val interceptorFactory: DejaVuInterceptor.Factory<E>,
        private val annotationProcessor: AnnotationProcessor,
        private val operationResolverFactory: RetrofitOperationResolver.Factory<E>
) : CallAdapter.Factory()
        where E : Throwable,
              E : NetworkErrorPredicate {

    override fun get(
            returnType: Type,
            annotations: Array<out Annotation>,
            retrofit: Retrofit
    ): CallAdapter<*, *>? {
        val rawType = getRawType(returnType)
        if (rawType != Flow::class.java) return null

        val flowType = getParameterUpperBound(0, returnType as ParameterizedType)
        val isDejaVuResult = getRawType(flowType) == DejaVuResult::class.java

        val responseType = if (isDejaVuResult) {
            getParameterUpperBound(0, flowType as ParameterizedType)
        } else {
            flowType
        }

        val operation = annotationProcessor.process(
                annotations.toList().toTypedArray(),
                getRawType(responseType)
        )

        return DejaVuCallAdapter<Any, E>(
                responseType,
                getRawType(responseType),
                isDejaVuResult,
                operation,
                interceptorFactory,
                operationResolverFactory
        )
    }
}
