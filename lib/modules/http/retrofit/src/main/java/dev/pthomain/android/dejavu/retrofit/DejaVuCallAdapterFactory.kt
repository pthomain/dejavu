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
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import io.reactivex.Observable
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * A Retrofit CallAdapter.Factory that integrates DejaVu caching.
 *
 * This factory intercepts Retrofit calls whose return types are Observable<T>
 * or Observable<DejaVuResult<T>> and applies the DejaVu caching interceptor chain.
 *
 * Cache instructions are resolved from (in decreasing priority):
 * 1. The operation predicate for the given RequestMetadata
 * 2. The request's DejaVuHeader
 * 3. The call's cache annotation (@Cache, @DoNotCache, @Invalidate, @Clear)
 *
 * @param interceptorFactory the factory that creates DejaVuInterceptor instances
 * @param annotationProcessor processes Retrofit annotations into cache operations
 * @param operationResolverFactory resolves the final cache operation for a call
 */
class DejaVuCallAdapterFactory<E>(
    private val interceptorFactory: DejaVuInterceptor.Factory<E>,
    private val annotationProcessor: AnnotationProcessor,
    private val operationResolverFactory: RetrofitOperationResolver.Factory<E>
) : CallAdapter.Factory() where E : Throwable, E : NetworkErrorPredicate {

    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        // Only handle Observable return types
        val rawType = getRawType(returnType)
        if (rawType != Observable::class.java) return null

        if (returnType !is ParameterizedType) {
            throw IllegalStateException(
                "Observable return type must be parameterized as Observable<T> or Observable<DejaVuResult<T>>"
            )
        }

        // Extract inner type: Observable<DejaVuResult<T>> or Observable<T>
        val observableType = getParameterUpperBound(0, returnType)
        val isDejaVuResult = getRawType(observableType) == DejaVuResult::class.java

        val responseType = if (isDejaVuResult && observableType is ParameterizedType) {
            getParameterUpperBound(0, observableType)
        } else {
            observableType
        }

        // Resolve operation from annotations
        val annotationOperation = annotationProcessor.process(
            annotations.filterIsInstance<Annotation>().toTypedArray(),
            getRawType(responseType)
        )

        // If no annotation found, let Retrofit's default adapters handle it
        if (annotationOperation == null && !isDejaVuResult) return null

        val methodDescription = "${getRawType(responseType).simpleName} (${annotationOperation?.type?.name ?: "none"})"

        return DejaVuCallAdapter<Any, E>(
            responseType,
            isDejaVuResult,
            annotationOperation,
            interceptorFactory,
            operationResolverFactory,
            methodDescription
        )
    }
}
