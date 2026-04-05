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

import dev.pthomain.android.dejavu.di.DejaVuComponent
import dev.pthomain.android.dejavu.retrofit.annotations.processor.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.glitchy.DejaVuReturnTypeParser
import dev.pthomain.android.dejavu.retrofit.glitchy.OperationReturnType
import dev.pthomain.android.dejavu.retrofit.glitchy.OperationReturnTypeParser
import dev.pthomain.android.dejavu.retrofit.interceptors.DejaVuRetrofitInterceptorFactory
import dev.pthomain.android.dejavu.retrofit.interceptors.HeaderInterceptor
import dev.pthomain.android.dejavu.retrofit.operation.RequestBodyConverter
import dev.pthomain.android.dejavu.retrofit.operation.RetrofitOperationResolver
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.core.Glitchy
import dev.pthomain.android.glitchy.retrofit.GlitchyRetrofit
import dev.pthomain.android.glitchy.retrofit.interceptors.RetrofitInterceptors
import retrofit2.CallAdapter

/**
 * Manual dependency injection component for the Retrofit module.
 * Replaces the previous Koin-based DejaVuRetrofitModule.
 */
internal class DejaVuRetrofitComponent<E>(
        private val parentComponent: DejaVuComponent<E>
) where E : Throwable,
        E : NetworkErrorPredicate {

    val annotationProcessor = AnnotationProcessor(
            parentComponent.logger,
            parentComponent.serialisationArgumentValidator
    )

    val dejaVuReturnTypeParser = DejaVuReturnTypeParser<E>()

    val operationReturnTypeParser = OperationReturnTypeParser<E>(
            dejaVuReturnTypeParser,
            annotationProcessor,
            parentComponent.logger
    )

    val requestBodyConverter = RequestBodyConverter()

    val operationResolverFactory = RetrofitOperationResolver.Factory<E>(
            parentComponent.operationPredicate::invoke,
            requestBodyConverter,
            parentComponent.logger
    )

    val retrofitInterceptors: RetrofitInterceptors<E> = RetrofitInterceptors.After(
            DejaVuRetrofitInterceptorFactory(
                    parentComponent.hasher,
                    parentComponent.dateFactory,
                    parentComponent.interceptorFactory,
                    operationResolverFactory
            )
    )

    val headerInterceptor = HeaderInterceptor()

    val callAdapterFactory: CallAdapter.Factory = Glitchy.builder<E>(parentComponent.errorFactory)
            .extend(GlitchyRetrofit.extension<E, OperationReturnType>())
            .withReturnTypeParser(operationReturnTypeParser)
            .withInterceptors(retrofitInterceptors)
            .build()
            .callAdapterFactory
}
