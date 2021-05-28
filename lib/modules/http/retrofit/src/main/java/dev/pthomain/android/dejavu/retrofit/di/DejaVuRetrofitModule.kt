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

package dev.pthomain.android.dejavu.retrofit.di

import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.RequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.retrofit.annotations.processor.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.glitchy.OperationMetadata
import dev.pthomain.android.dejavu.retrofit.glitchy.OperationReturnTypeParser
import dev.pthomain.android.dejavu.retrofit.interceptors.DejaVuRetrofitInterceptorFactory
import dev.pthomain.android.dejavu.retrofit.interceptors.HeaderInterceptor
import dev.pthomain.android.dejavu.retrofit.operation.RequestBodyConverter
import dev.pthomain.android.dejavu.retrofit.operation.RetrofitOperationResolver
import dev.pthomain.android.glitchy.core.interceptor.interceptors.base.Interceptors
import dev.pthomain.android.glitchy.core.interceptor.interceptors.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.flow.interceptors.base.FlowInterceptors
import dev.pthomain.android.glitchy.retrofit.flow.GlitchyRetrofitFlow
import dev.pthomain.android.glitchy.retrofit.flow.adapter.RetrofitFlowCallAdapterFactory
import dev.pthomain.android.glitchy.retrofit.flow.type.FlowReturnTypeParser
import dev.pthomain.android.glitchy.retrofit.interceptors.RetrofitInterceptorFactory
import dev.pthomain.android.glitchy.retrofit.interceptors.RetrofitMetadata
import dev.pthomain.android.glitchy.retrofit.type.DefaultOutcomeReturnTypeParser
import dev.pthomain.android.glitchy.retrofit.type.ReturnTypeParser
import org.koin.core.qualifier.named
import org.koin.dsl.module

class DejaVuRetrofitModule<E>
        where E : Throwable,
              E : NetworkErrorPredicate {

    val module = module {

        single { AnnotationProcessor(get(), get()) }

        single {
            DefaultOutcomeReturnTypeParser.getDefaultInstance(FlowReturnTypeParser) {
                it == DejaVuResult::class.java
            }
        }

        single<ReturnTypeParser<OperationMetadata>> {
            OperationReturnTypeParser<E>(
                    get(),
                    get(),
                    get()
            )
        }

        single { RequestBodyConverter() }

        single {
            RetrofitOperationResolver.Factory<E>(
                    get<(RequestMetadata<*>) -> Remote?>(named("operationMapper"))::invoke,
                    get<RequestBodyConverter>(),
                    get()
            )
        }

        single<Interceptors<RetrofitMetadata<OperationMetadata>, RetrofitInterceptorFactory<OperationMetadata>>> {
            FlowInterceptors.After(
                    DejaVuRetrofitInterceptorFactory<E>(
                            get(),
                            get(named("dateFactory")),
                            get(),
                            get()
                    )
            )
        }

        single { HeaderInterceptor() }

        single {
            GlitchyRetrofitFlow.Custom.builder<E, OperationMetadata>(
                    get(),
                    RetrofitFlowCallAdapterFactory(),
                    get(),
                    get()
            ).build().callAdapterFactory
        }
    }

}