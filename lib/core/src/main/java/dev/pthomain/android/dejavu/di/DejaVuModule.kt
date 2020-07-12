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
import dev.pthomain.android.dejavu.cache.metadata.response.TransientResponse
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.Hasher
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.RequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.interceptors.CacheInterceptor
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.interceptors.NetworkInterceptor
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseFactory
import dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.dejavu.serialisation.SerialisationArgumentValidator
import dev.pthomain.android.glitchy.core.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.*

typealias DateFactory = (Long?) -> Date

internal fun Date.ellapsed(dateFactory: DateFactory) =
        (dateFactory(null).time - time).toInt()

internal class DejaVuModule<E>(
        context: Context,
        logger: Logger,
        private val errorFactory: ErrorFactory<E>,
        persistenceModule: PersistenceManager.ModuleProvider,
        private val operationPredicate: (RequestMetadata<*>) -> Remote?,
        private val durationPredicate: (TransientResponse<*>) -> Int?,
) where E : Throwable,
        E : NetworkErrorPredicate {

    val modules = persistenceModule.modules + module {

        single { context.applicationContext }

        single { logger }

        single(named("operationPredicate")) { operationPredicate }

        single<DateFactory>(named("dateFactory")) {
            { if (it == null) Date() else Date(it) }
        }

        single { errorFactory }

        single<(String) -> Uri> { Uri::parse }

        single {
            Hasher(
                    get(),
                    get()
            )
        }

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
                    get(named("dateFactory"))
            )
        }

        single {
            EmptyResponseFactory<E>(
                    get(),
                    get(named("dateFactory"))
            )
        }

        single { SerialisationArgumentValidator(persistenceModule.decorators) }

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
    }

}