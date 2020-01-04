/*
 *
 *  Copyright (C) 2017 Pierre Thomain
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
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.injection.Function1
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.interceptors.cache.instruction.OperationSerialiser
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.util.*
import javax.inject.Singleton

@Module
internal abstract class RetrofitModule<E>
        where E : Exception,
              E : NetworkErrorPredicate {

    @Provides
    @Singleton
    fun provideDefaultAdapterFactory() =
            RxJava2CallAdapterFactory.create()!!


    @Provides
    @Singleton
    fun provideRetrofitCallAdapterFactory(configuration: DejaVuConfiguration<E>,
                                          dateFactory: Function1<Long?, Date>,
                                          logger: Logger,
                                          defaultAdapterFactory: RxJava2CallAdapterFactory,
                                          dejaVuInterceptorFactory: DejaVuInterceptor.Factory<E>,
                                          annotationProcessor: AnnotationProcessor<E>) =
            RetrofitCallAdapterFactory(
                    configuration,
                    defaultAdapterFactory,
                    { dejaVuFactory, methodDescription, responseClass, rxType, operation, rxCallAdapter ->
                        RetrofitCallAdapter(
                                configuration,
                                responseClass,
                                dejaVuFactory,
                                OperationSerialiser(),
                                RequestBodyConverter(),
                                logger,
                                methodDescription,
                                rxType,
                                operation,
                                rxCallAdapter
                        )
                    },
                    dateFactory::get,
                    dejaVuInterceptorFactory,
                    OperationSerialiser(),
                    RequestBodyConverter(),
                    annotationProcessor,
                    logger
            )

    @Provides
    @Singleton
    fun provideAnnotationProcessor(logger: Logger) =
            AnnotationProcessor<E>(logger)

}