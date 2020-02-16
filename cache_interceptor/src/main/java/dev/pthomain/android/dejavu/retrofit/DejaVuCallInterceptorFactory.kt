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
import dev.pthomain.android.dejavu.retrofit.glitchy.OperationReturnType
import dev.pthomain.android.glitchy.interceptor.Interceptor
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.retrofit.type.ParsedType
import retrofit2.Call

internal class DejaVuCallInterceptorFactory<E>(
        private val operationResolverFactory: OperationResolver.Factory<E>,
        private val dejaVuInterceptorFactory: DejaVuInterceptor.Factory<E>
) : Interceptor.CallFactory<E>
        where E : Throwable,
              E : NetworkErrorPredicate {

    override fun <M> create(parsedType: ParsedType<M>,
                            call: Call<Any>): Interceptor? {
        with(parsedType.metadata) {
            if (this is OperationReturnType) {
                val resolver = operationResolverFactory.create(
                        methodDescription,
                        dejaVuReturnType.responseClass,
                        annotationOperation
                )

                resolver.getResolvedOperation(call)?.let {
                    dejaVuInterceptorFactory.create(
                            dejaVuReturnType.isDejaVuResult,
                            it.operation,
                            it.requestMetadata
                    )
                }
            }
        }

        return null
    }
}
