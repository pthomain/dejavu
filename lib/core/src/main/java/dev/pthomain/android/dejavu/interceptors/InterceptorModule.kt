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

package dev.pthomain.android.dejavu.interceptors

import android.content.Context
import dagger.Module
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.DejaVu.Configuration
import dev.pthomain.android.dejavu.cache.CacheManager
import dev.pthomain.android.dejavu.cache.Hasher
import dev.pthomain.android.dejavu.di.Function1
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseFactory
import dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
import dev.pthomain.android.dejavu.retrofit.OperationResolver
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import java.util.*
import javax.inject.Singleton

@Module
abstract class InterceptorModule<E>
        where E : Throwable,
              E : NetworkErrorPredicate {

    @Provides
    @Singleton
    internal fun provideNetworkInterceptorFactory(context: Context,
                                                  logger: Logger,
                                                  dateFactory: Function1<Long?, Date>) =
            NetworkInterceptor.Factory<E>(
                    context,
                    logger,
                    dateFactory::get
            )

    @Provides
    @Singleton
    internal fun provideCacheInterceptorFactory(cacheManager: CacheManager<E>) =
            CacheInterceptor.Factory(cacheManager)

    @Provides
    @Singleton
    internal fun provideResponseInterceptor(configuration: Configuration<E>,
                                            logger: Logger,
                                            dateFactory: Function1<Long?, Date>,
                                            emptyResponseFactory: EmptyResponseFactory<E>) =
            ResponseInterceptor.Factory(
                    configuration,
                    logger,
                    dateFactory::get,
                    emptyResponseFactory
            )

    @Provides
    @Singleton
    internal fun provideEmptyResponseFactory(configuration: Configuration<E>) =
            EmptyResponseFactory(configuration.errorFactory)

    @Provides
    @Singleton
    internal fun provideOkHttpHeaderInterceptor() =
            HeaderInterceptor()

    @Provides
    @Singleton
    internal fun provideDejaVuInterceptorFactory(hasher: Hasher,
                                                 configuration: Configuration<E>,
                                                 dateFactory: Function1<Long?, Date>,
                                                 networkInterceptorFactory: NetworkInterceptor.Factory<E>,
                                                 cacheInterceptorFactory: CacheInterceptor.Factory<E>,
                                                 operationResolverFactory: OperationResolver.Factory<E>,
                                                 responseInterceptorFactory: ResponseInterceptor.Factory<E>) =
            DejaVuInterceptor.Factory(
                    hasher,
                    dateFactory::get,
                    networkInterceptorFactory,
                    cacheInterceptorFactory,
                    responseInterceptorFactory,
                    operationResolverFactory,
                    configuration
            )

}
