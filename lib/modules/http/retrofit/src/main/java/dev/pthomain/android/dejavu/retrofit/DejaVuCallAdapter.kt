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

import dev.pthomain.android.dejavu.cache.metadata.token.instruction.PlainRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.error.Outcome
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.retrofit.operation.RetrofitOperationResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.HttpException
import java.lang.reflect.Type

/**
 * A Retrofit CallAdapter that converts a Call<R> into a Flow<*>,
 * optionally passing it through the DejaVu interceptor chain for caching.
 */
class DejaVuCallAdapter<R : Any, E>(
        private val responseType: Type,
        private val isDejaVuResult: Boolean,
        private val operation: Operation?,
        private val interceptorFactory: DejaVuInterceptor.Factory<E>,
        private val operationResolverFactory: RetrofitOperationResolver.Factory<E>
) : CallAdapter<R, Flow<*>> where E : Throwable, E : NetworkErrorPredicate {

    override fun responseType(): Type = responseType

    @Suppress("UNCHECKED_CAST")
    override fun adapt(call: Call<R>): Flow<*> {
        // Create upstream Flow from Retrofit Call, wrapping in Outcome
        val upstream: Flow<Any> = flow {
            try {
                val response = call.clone().execute()
                if (response.isSuccessful && response.body() != null) {
                    emit(Outcome.Success(response.body()!!) as Any)
                } else {
                    emit(Outcome.Error(HttpException(response)) as Any)
                }
            } catch (e: Exception) {
                emit(Outcome.Error(e) as Any)
            }
        }.flowOn(Dispatchers.IO)

        if (operation == null) {
            // No cache operation - return raw response as Flow
            return upstream.map { outcome ->
                when (outcome) {
                    is Outcome.Success<*> -> outcome.response
                    is Outcome.Error<*> -> throw outcome.exception
                    else -> throw IllegalStateException("Unexpected outcome type")
                }
            }
        }

        // Build request metadata
        val requestMetadata = PlainRequestMetadata(
                responseClass = CallAdapter.Factory.getRawType(responseType) as Class<R>,
                url = call.request().url.toString(),
                requestBody = call.request().body?.toString()
        )

        // Create and apply DejaVu interceptor
        val interceptor = interceptorFactory.create<R>(
                isDejaVuResult,
                operation,
                requestMetadata
        )

        // Apply the Flow-based interceptor chain
        return interceptor.intercept(upstream)
    }
}
