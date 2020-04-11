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

package dev.pthomain.android.dejavu

import android.content.Context
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.configuration.Serialiser
import dev.pthomain.android.dejavu.injection.DejaVuComponent
import dev.pthomain.android.dejavu.injection.glitch.DaggerGlitchDejaVuComponent
import dev.pthomain.android.dejavu.injection.glitch.GlitchDejaVuModule
import dev.pthomain.android.dejavu.interceptors.cache.persistence.statistics.StatisticsCompiler
import dev.pthomain.android.dejavu.interceptors.error.DejaVuGlitchFactory
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.interceptor.error.glitch.GlitchFactory

/**
 * Contains the Retrofit call adapter, DejaVuInterceptor factory and current global configuration.
 */
class DejaVu<E> internal constructor(component: DejaVuComponent<E>)
    : StatisticsCompiler by component.statisticsCompiler()
        where E : Throwable,
              E : NetworkErrorPredicate {

    /**
     * Provides the current configuration.
     */
    val configuration = component.configuration()

    /**
     * Provides the OkHttp header interceptor to add to your OkHttp setup if you plan
     * to use header instructions.
     *
     * NB: Header instructions would work without this interceptor but the header would be sent
     * alongside the request to the API server. This might not be an issue, but if it is,
     * this interceptor will remove the header before the network call is made.
     */
    val headerInterceptor = component.headerInterceptor()

    /**
     * Provides the adapter factory to use with Retrofit.
     */
    val retrofitCallAdapterFactory = component.retrofitCallAdapterFactory()

    /**
     * Provides a generic DejaVuInterceptor factory to use with any Observable / Single / CacheOperation.
     * @see dev.pthomain.android.dejavu.interceptors.RxType
     */
    val dejaVuInterceptorFactory = component.dejaVuInterceptorFactory()

    companion object {

        /**
         * Use this value to provide the cache instruction as a header (this will override any existing call annotation)
         */
        const val DejaVuHeader = "DejaVuHeader" //TODO find a way to strip this after processing

        /**
         * @return Builder for DejaVuConfiguration
         */
        fun <E> builder(context: Context,
                        serialiser: Serialiser,
                        errorFactory: ErrorFactory<E>,
                        componentProvider: (DejaVuConfiguration<E>) -> DejaVuComponent<E>)
                where E : Throwable,
                      E : NetworkErrorPredicate = DejaVuConfiguration.Builder(
                errorFactory,
                context,
                serialiser,
                componentProvider
        )

        /**
         * @return Builder for DejaVuConfiguration
         */
        fun defaultBuilder(context: Context,
                           serialiser: Serialiser) = builder(
                context,
                serialiser,
                DejaVuGlitchFactory(GlitchFactory())
        ) {
            DaggerGlitchDejaVuComponent
                    .builder()
                    .glitchDejaVuModule(GlitchDejaVuModule(it))
                    .build()
        }

    }
}
