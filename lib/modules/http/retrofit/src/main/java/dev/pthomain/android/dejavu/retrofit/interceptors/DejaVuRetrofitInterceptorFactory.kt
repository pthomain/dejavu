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

package dev.pthomain.android.dejavu.retrofit.interceptors

import dev.pthomain.android.dejavu.cache.metadata.token.instruction.Hasher
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.PlainRequestMetadata
import dev.pthomain.android.dejavu.di.DateFactory
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.retrofit.glitchy.OperationReturnType
import dev.pthomain.android.dejavu.retrofit.operation.RetrofitOperationResolver
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.retrofit.interceptors.RetrofitInterceptor
import dev.pthomain.android.glitchy.retrofit.type.ParsedType
import io.reactivex.Observable
import retrofit2.Call

/**
 * Factory providing instances of DejaVuInterceptor
 *
 * @param hasher the class handling the request hashing for unicity
 * @param dateFactory the factory transforming timestamps to dates
 */
class DejaVuRetrofitInterceptorFactory<E> internal constructor(
        private val hasher: Hasher,
        private val dateFactory: DateFactory,
        private val dejaVuInterceptorFactory: DejaVuInterceptor.Factory<E>,
        private val operationResolverFactory: RetrofitOperationResolver.Factory<E>
) : RetrofitInterceptor.Factory<E>
        where E : Throwable,
              E : NetworkErrorPredicate {

    override fun <M> create(
            parsedType: ParsedType<M>,
            call: Call<Any>
    ) =
            (parsedType.metadata as? OperationReturnType)?.run {
                operationResolverFactory.create(
                        methodDescription,
                        dejaVuReturnType.responseClass,
                        annotationOperation
                ).getResolvedOperation(call)?.run {
                    dejaVuInterceptorFactory.create(
                            dejaVuReturnType.isDejaVuResult,
                            operation,
                            requestMetadata as PlainRequestMetadata<out Any>
                    )
                }?.let { dejaVuInterceptor ->
                    object : RetrofitInterceptor.SimpleInterceptor() {
                        override fun apply(upstream: Observable<Any>) =
                                dejaVuInterceptor.apply(upstream)
                    }
                }
            }

}
