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
import dev.pthomain.android.dejavu.configuration.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.configuration.instruction.CacheOperationSerialiser
import dev.pthomain.android.dejavu.configuration.instruction.Operation
import dev.pthomain.android.dejavu.injection.Function1
import dev.pthomain.android.dejavu.injection.Function3
import dev.pthomain.android.dejavu.injection.Function6
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.error.ErrorInterceptor
import dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType
import retrofit2.CallAdapter
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
    fun provideRetrofitCallAdapterInnerFactory(configuration: DejaVuConfiguration<E>) =
            object : Function6<DejaVuInterceptor.Factory<E>, Logger, String, Class<*>, Operation?, CallAdapter<Any, Any>, RetrofitCallAdapter<E>> {
                override fun get(
                        t1: DejaVuInterceptor.Factory<E>,
                        t2: Logger,
                        t3: String,
                        t4: Class<*>,
                        t5: Operation?,
                        t6: CallAdapter<Any, Any>
                ) = RetrofitCallAdapter(
                        configuration,
                        t4,
                        t1,
                        CacheOperationSerialiser(),
                        RequestBodyConverter(),
                        t2,
                        t3,
                        t5,
                        t6
                )
            }

    @Provides
    @Singleton
    fun provideRetrofitCallAdapterFactory(dateFactory: Function1<Long?, Date>,
                                          logger: Logger,
                                          innerFactory: Function6<DejaVuInterceptor.Factory<E>, Logger, String, Class<*>, Operation?, CallAdapter<Any, Any>, RetrofitCallAdapter<E>>,
                                          defaultAdapterFactory: RxJava2CallAdapterFactory,
                                          dejaVuInterceptorFactory: DejaVuInterceptor.Factory<E>,
                                          processingErrorAdapterFactory: ProcessingErrorAdapter.Factory<E>,
                                          annotationProcessor: AnnotationProcessor<E>) =
            RetrofitCallAdapterFactory(
                    defaultAdapterFactory,
                    innerFactory::get,
                    dateFactory::get,
                    dejaVuInterceptorFactory,
                    annotationProcessor,
                    processingErrorAdapterFactory,
                    logger
            )

    @Provides
    @Singleton
    fun provideProcessingErrorAdapterFactory(errorInterceptorFactory: Function1<CacheToken, ErrorInterceptor<E>>,
                                             dateFactory: Function1<Long?, Date>,
                                             responseInterceptorFactory: Function3<CacheToken, RxType, Long, ResponseInterceptor<E>>) =
            ProcessingErrorAdapter.Factory(
                    errorInterceptorFactory::get,
                    responseInterceptorFactory::get,
                    dateFactory::get
            )

    @Provides
    @Singleton
    fun provideAnnotationProcessor(configuration: DejaVuConfiguration<E>) =
            AnnotationProcessor(configuration)

}