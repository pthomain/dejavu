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

package dev.pthomain.android.dejavu.di

import android.content.Context
import android.net.Uri
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.CacheManager
import dev.pthomain.android.dejavu.cache.CacheMetadataManager
import dev.pthomain.android.dejavu.cache.TransientResponse
import dev.pthomain.android.dejavu.interceptors.CacheInterceptor
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.interceptors.HeaderInterceptor
import dev.pthomain.android.dejavu.interceptors.NetworkInterceptor
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseFactory
import dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
import dev.pthomain.android.dejavu.retrofit.OperationResolver
import dev.pthomain.android.dejavu.retrofit.RequestBodyConverter
import dev.pthomain.android.dejavu.retrofit.annotations.processor.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.glitchy.DejaVuReturnTypeParser
import dev.pthomain.android.dejavu.retrofit.glitchy.OperationReturnType
import dev.pthomain.android.dejavu.retrofit.glitchy.OperationReturnTypeParser
import dev.pthomain.android.dejavu.shared.di.SharedModule
import dev.pthomain.android.dejavu.shared.persistence.PersistenceManager
import dev.pthomain.android.dejavu.shared.token.instruction.RequestMetadata
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Remote
import dev.pthomain.android.glitchy.Glitchy
import dev.pthomain.android.glitchy.interceptor.Interceptors
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.retrofit.type.ReturnTypeParser
import org.koin.core.qualifier.named
import org.koin.dsl.module

class DejaVuModule<E>(
        context: Context,
        logger: Logger,
        private val errorFactory: ErrorFactory<E>,
        persistenceModule: PersistenceManager.ModuleProvider,
        private val operationPredicate: (RequestMetadata<*>) -> Remote?,
        private val durationPredicate: (TransientResponse<*>) -> Int?
) where E : Throwable,
        E : NetworkErrorPredicate {

    private val sharedModule = SharedModule(context, logger).module

    val modules = sharedModule + persistenceModule.modules + module {

        single { errorFactory }

        single<(String) -> Uri> { Uri::parse }

        single {
            NetworkInterceptor.Factory<E>(
                    get(),
                    get(),
                    get(named("dateFactory"))
            )
        }

        single {
            CacheInterceptor.Factory<E>(get())
        }

        single {
            CacheMetadataManager<E>(
                    get(),
                    get(),
                    get(named("dateFactory")),
                    durationPredicate::invoke,
                    get()
            )
        }

        single {
            CacheManager<E>(
                    get(),
                    get(),
                    get(),
                    get(named("dateFactory")),
                    get()
            )
        }

        single {
            ResponseInterceptor.Factory<E>(
                    get(),
                    get(named("dateFactory")),
                    get()
            )
        }

        single {
            EmptyResponseFactory<E>(get())
        }

        single { HeaderInterceptor() }

        single {
            DejaVuInterceptor.Factory<E>(
                    get(),
                    get(),
                    get(named("dateFactory")),
                    get(),
                    get(),
                    get(),
                    get()
            )
        }

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
            OperationResolver.Factory<E>(
                    operationPredicate::invoke,
                    get<RequestBodyConverter>(),
                    get()
            )
        }

        single<Interceptors<E>> {
            Interceptors.After(get<DejaVuInterceptor.Factory<E>>().glitchyFactory)
        }

        single {
            Glitchy.createCallAdapterFactory<E, OperationReturnType>(
                    get(),
                    get(),
                    get()
            )
        }
    }

}