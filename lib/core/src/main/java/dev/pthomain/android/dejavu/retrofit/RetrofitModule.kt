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

import dagger.Module
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.DejaVu.Configuration
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.retrofit.annotations.processor.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.glitchy.DejaVuReturnTypeParser
import dev.pthomain.android.dejavu.retrofit.glitchy.OperationReturnTypeParser
import dev.pthomain.android.glitchy.Glitchy
import dev.pthomain.android.glitchy.interceptor.Interceptors
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import retrofit2.CallAdapter
import javax.inject.Singleton

@Module
abstract class RetrofitModule<E> where E : Throwable,
                                       E : NetworkErrorPredicate {

    @Provides
    @Singleton
    internal fun provideAnnotationProcessor(logger: Logger) =
            AnnotationProcessor<E>(logger)

    @Provides
    @Singleton
    internal fun provideDejaVuReturnTypeParser() =
            DejaVuReturnTypeParser<E>()

    @Provides
    @Singleton
    internal fun provideOperationReturnTypeParser(
            dejaVuTypeParser: DejaVuReturnTypeParser<E>,
            annotationProcessor: AnnotationProcessor<E>,
            logger: Logger
    ) =
            OperationReturnTypeParser(
                    dejaVuTypeParser,
                    annotationProcessor,
                    logger
            )

    @Provides
    @Singleton
    internal fun provideRequestBodyConverter() =
            RequestBodyConverter()

    @Provides
    @Singleton
    internal fun provideOperationResolverFactory(
            configuration: Configuration<E>,
            requestBodyConverter: RequestBodyConverter,
            logger: Logger
    ) =
            OperationResolver.Factory(
                    configuration,
                    requestBodyConverter,
                    logger
            )

    @Provides
    @Singleton
    internal fun provideCallAdapterFactory(
            errorFactory: ErrorFactory<E>,
            operationReturnTypeParser: OperationReturnTypeParser<E>,
            dejaVuCallInterceptorFactory: DejaVuInterceptor.Factory<E>,
            logger: Logger
    ): CallAdapter.Factory =
            Glitchy.createCallAdapterFactory(
                    errorFactory,
                    operationReturnTypeParser,
                    Interceptors.After(dejaVuCallInterceptorFactory.glitchyFactory),
                    logger
            )

}