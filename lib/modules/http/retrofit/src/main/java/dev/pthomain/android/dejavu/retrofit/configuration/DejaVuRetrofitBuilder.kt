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

package dev.pthomain.android.dejavu.retrofit.configuration

import dev.pthomain.android.dejavu.configuration.ExtensionBuilder
import dev.pthomain.android.dejavu.di.DejaVuComponent
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.retrofit.DejaVuCallAdapterFactory
import dev.pthomain.android.dejavu.retrofit.DejaVuRetrofit
import dev.pthomain.android.dejavu.retrofit.annotations.processor.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.interceptors.HeaderInterceptor
import dev.pthomain.android.dejavu.retrofit.operation.RequestBodyConverter
import dev.pthomain.android.dejavu.retrofit.operation.RetrofitOperationResolver

/**
 * Builder for creating DejaVuRetrofit instances.
 *
 * This builder integrates with the core DejaVu builder pattern via the
 * ExtensionBuilder interface, receiving a DejaVuComponent from the parent builder
 * and resolving the necessary dependencies.
 */
class DejaVuRetrofitBuilder<E> internal constructor()
    where E : Throwable,
          E : NetworkErrorPredicate {

    internal var component: DejaVuComponent<E>? = null

    internal fun accept(component: DejaVuComponent<E>) = apply {
        this.component = component
    }

    /**
     * Builds a DejaVuRetrofit instance by resolving dependencies from the
     * DejaVuComponent and creating the Retrofit call adapter factory.
     */
    fun build(): DejaVuRetrofit<E> {
        val component = this.component
            ?: throw IllegalStateException("This builder needs to call DejaVuBuilder::extend")

        val interceptorFactory = component.interceptorFactory
        val logger = component.logger
        val serialisationArgumentValidator = component.serialisationArgumentValidator
        val operationPredicate = component.operationPredicate

        val annotationProcessor = AnnotationProcessor(logger, serialisationArgumentValidator)

        val operationResolverFactory = RetrofitOperationResolver.Factory<E>(
            operationPredicate,
            RequestBodyConverter(),
            logger
        )

        val callAdapterFactory = DejaVuCallAdapterFactory(
            interceptorFactory,
            annotationProcessor,
            operationResolverFactory
        )

        return DejaVuRetrofit(
            callAdapterFactory,
            HeaderInterceptor(),
            interceptorFactory
        )
    }
}

/**
 * Internal adapter that bridges DejaVuRetrofitBuilder to the ExtensionBuilder interface.
 */
class DejaVuRetrofitExtensionBuilder<E> internal constructor()
    : ExtensionBuilder<DejaVuRetrofitExtensionBuilder<E>, DejaVuRetrofitBuilder<E>, E>
    where E : Throwable,
          E : NetworkErrorPredicate {

    private val retrofitBuilder = DejaVuRetrofitBuilder<E>()

    override fun accept(component: DejaVuComponent<E>) = apply {
        retrofitBuilder.accept(component)
    }

    override fun build(): DejaVuRetrofitBuilder<E> = retrofitBuilder
}
