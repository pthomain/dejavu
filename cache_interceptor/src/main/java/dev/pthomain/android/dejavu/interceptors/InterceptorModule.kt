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
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.injection.Function1
import dev.pthomain.android.dejavu.interceptors.cache.CacheInterceptor
import dev.pthomain.android.dejavu.interceptors.cache.CacheManager
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.error.ErrorInterceptor
import dev.pthomain.android.dejavu.interceptors.network.NetworkInterceptor
import dev.pthomain.android.dejavu.interceptors.response.DejaVuResult
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseWrapperFactory
import dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
import dev.pthomain.android.dejavu.retrofit.OperationResolver
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import io.reactivex.subjects.PublishSubject
import java.util.*
import javax.inject.Singleton

@Module
internal abstract class InterceptorModule<E>
        where E : Throwable,
              E : NetworkErrorPredicate {

    @Provides
    @Singleton
    fun provideErrorInterceptorFactory(configuration: DejaVuConfiguration<E>,
                                       context: Context,
                                       emptyResponseWrapperFactory: EmptyResponseWrapperFactory<E>,
                                       logger: Logger,
                                       dateFactory: Function1<Long?, Date>) =
            ErrorInterceptor.Factory(
                    context,
                    configuration.errorFactory,
                    emptyResponseWrapperFactory,
                    logger,
                    dateFactory::get
            )

    @Provides
    @Singleton
    fun provideNetworkInterceptorFactory(context: Context,
                                         logger: Logger,
                                         dateFactory: Function1<Long?, Date>) =
            NetworkInterceptor.Factory<E>(
                    context,
                    logger,
                    dateFactory::get
            )

    @Provides
    @Singleton
    fun provideCacheInterceptorFactory(dateFactory: Function1<Long?, Date>,
                                       cacheManager: CacheManager<E>) =
            CacheInterceptor.Factory(
                    dateFactory::get,
                    cacheManager
            )

    @Provides
    @Singleton
    fun provideResponseInterceptor(configuration: DejaVuConfiguration<E>,
                                   logger: Logger,
                                   dateFactory: Function1<Long?, Date>,
                                   metadataSubject: PublishSubject<DejaVuResult<*>>,
                                   emptyResponseWrapperFactory: EmptyResponseWrapperFactory<E>) =
            ResponseInterceptor.Factory(
                    configuration,
                    logger,
                    dateFactory::get,
                    metadataSubject,
                    emptyResponseWrapperFactory
            )

    @Provides
    @Singleton
    fun provideEmptyResponseFactory(configuration: DejaVuConfiguration<E>,
                                    dateFactory: Function1<Long?, Date>) =
            EmptyResponseWrapperFactory(
                    configuration.errorFactory,
                    dateFactory::get
            )


    @Provides
    @Singleton
    fun provideOkHttpHeaderInterceptor() =
            HeaderInterceptor()

    @Provides
    @Singleton
    fun provideDejaVuInterceptorFactory(hasher: Hasher,
                                        configuration: DejaVuConfiguration<E>,
                                        dateFactory: Function1<Long?, Date>,
                                        networkInterceptorFactory: NetworkInterceptor.Factory<E>,
                                        errorInterceptorFactory: ErrorInterceptor.Factory<E>,
                                        cacheInterceptorFactory: CacheInterceptor.Factory<E>,
                                        operationResolverFactory: OperationResolver.Factory<E>,
                                        responseInterceptorFactory: ResponseInterceptor.Factory<E>) =
            DejaVuInterceptor.Factory(
                    hasher,
                    dateFactory::get,
                    errorInterceptorFactory,
                    networkInterceptorFactory,
                    cacheInterceptorFactory,
                    responseInterceptorFactory,
                    operationResolverFactory,
                    configuration
            )

}
