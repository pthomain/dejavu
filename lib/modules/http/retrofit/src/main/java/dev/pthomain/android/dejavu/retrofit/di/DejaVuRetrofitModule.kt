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

import dev.pthomain.android.dejavu.cache.metadata.token.instruction.RequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.retrofit.annotations.processor.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.glitchy.DejaVuReturnTypeParser
import dev.pthomain.android.dejavu.retrofit.glitchy.OperationReturnType
import dev.pthomain.android.dejavu.retrofit.glitchy.OperationReturnTypeParser
import dev.pthomain.android.dejavu.retrofit.interceptors.DejaVuRetrofitInterceptorFactory
import dev.pthomain.android.dejavu.retrofit.interceptors.HeaderInterceptor
import dev.pthomain.android.dejavu.retrofit.operation.RequestBodyConverter
import dev.pthomain.android.dejavu.retrofit.operation.RetrofitOperationResolver
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.retrofit.GlitchyRetrofit
import dev.pthomain.android.glitchy.retrofit.interceptors.RetrofitInterceptors
import dev.pthomain.android.glitchy.retrofit.type.ReturnTypeParser
import org.koin.core.qualifier.named
import org.koin.dsl.module

class DejaVuRetrofitModule<E>
        where E : Throwable,
              E : NetworkErrorPredicate {

    val module = module {

        single { AnnotationProcessor<E>(get()) }

        single { DejaVuReturnTypeParser<E>() }

        single<ReturnTypeParser<OperationReturnType>> {
            OperationReturnTypeParser<E>(
                    get(),
                    get(),
                    get()
            )
        }

        single { RequestBodyConverter() }

        single {
            RetrofitOperationResolver.Factory<E>(
                    get<(RequestMetadata<*>) -> Operation.Remote?>(named("operationPredicate"))::invoke,
                    get<RequestBodyConverter>(),
                    get()
            )
        }

        single<RetrofitInterceptors<E>?> {
            RetrofitInterceptors.After(
                    DejaVuRetrofitInterceptorFactory(
                            get(),
                            get(named("dateFactory")),
                            get(),
                            get()
                    )
            )
        }

        single { HeaderInterceptor() }

        single {
            GlitchyRetrofit.createCallAdapterFactory<E, OperationReturnType>(
                    get(),
                    get(),
                    get()
            )
        }
    }

}