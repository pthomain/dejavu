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

import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.retrofit.operation.RetrofitOperationResolver
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.core.interceptor.outcome.Outcome
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.CallAdapter
import java.lang.reflect.Type

/**
 * A Retrofit CallAdapter that converts a Retrofit Call into an Observable
 * and applies the DejaVu interceptor chain for caching.
 *
 * This adapter:
 * 1. Converts the Retrofit Call<R> into an Observable<Outcome<R>>
 * 2. Resolves the cache operation (from predicate, header, or annotation)
 * 3. Applies the DejaVuInterceptor to the observable stream
 *
 * @param R the response type
 * @param E the error type
 * @param responseType the response type for Retrofit deserialization
 * @param isDejaVuResult whether the return type is wrapped in DejaVuResult
 * @param annotationOperation the cache operation from annotations, if any
 * @param interceptorFactory factory for creating DejaVuInterceptor instances
 * @param operationResolverFactory factory for creating RetrofitOperationResolver instances
 * @param methodDescription a description of the method for logging
 */
internal class DejaVuCallAdapter<R : Any, E>(
    private val responseType: Type,
    private val isDejaVuResult: Boolean,
    private val annotationOperation: Operation?,
    private val interceptorFactory: DejaVuInterceptor.Factory<E>,
    private val operationResolverFactory: RetrofitOperationResolver.Factory<E>,
    private val methodDescription: String
) : CallAdapter<R, Observable<*>> where E : Throwable, E : NetworkErrorPredicate {

    override fun responseType(): Type = responseType

    @Suppress("UNCHECKED_CAST")
    override fun adapt(call: Call<R>): Observable<*> {
        // Create the operation resolver for this specific call
        val operationResolver = operationResolverFactory.create(
            methodDescription,
            responseType as Class<R>,
            annotationOperation
        )

        // Resolve the operation (predicate > header > annotation)
        val resolvedOperation = operationResolver.getResolvedOperation(call as Call<Any>)
            ?: return Observable.fromCallable {
                // No cache operation found -- execute the call directly
                val response = call.clone().execute()
                if (response.isSuccessful) {
                    response.body()!!
                } else {
                    throw retrofit2.HttpException(response)
                }
            }

        val operation = resolvedOperation.operation
        val requestMetadata = resolvedOperation.requestMetadata

        // Convert the Retrofit Call to an Observable emitting Outcome
        val upstream: Observable<Any> = Observable.fromCallable {
            val response = call.clone().execute()
            if (response.isSuccessful) {
                Outcome.Success(response.body()!!) as Any
            } else {
                throw retrofit2.HttpException(response)
            }
        }

        // Create and apply the DejaVu interceptor
        val interceptor = interceptorFactory.create(
            isDejaVuResult,
            operation,
            requestMetadata
        )

        return upstream.compose(interceptor)
    }
}
