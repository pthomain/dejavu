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

package dev.pthomain.android.dejavu

import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.configuration.NetworkErrorProvider
import dev.pthomain.android.dejavu.injection.component.CacheComponent
import dev.pthomain.android.dejavu.injection.component.DaggerDefaultCacheComponent
import dev.pthomain.android.dejavu.injection.module.DefaultCacheModule
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.interceptors.internal.error.Glitch
import dev.pthomain.android.dejavu.interceptors.internal.error.GlitchFactory
import dev.pthomain.android.dejavu.response.CacheMetadata
import dev.pthomain.android.dejavu.retrofit.RetrofitCallAdapterFactory
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Contains the Retrofit call adapter, DejaVuInterceptor factory and current global configuration.
 */
class DejaVu<E> internal constructor(private val component: CacheComponent<E>)
        where E : Exception,
              E : NetworkErrorProvider {

    /**
     * Provides the current configuration
     */
    val configuration: CacheConfiguration<E> = component.configuration()

    /**
     * Provides the adapter factory to use with Retrofit
     */
    val retrofitCallAdapterFactory: RetrofitCallAdapterFactory<E> = component.retrofitCacheAdapterFactory()

    /**
     * Provides a generic DejaVuInterceptor factory to use with any Observable/Single/Completable
     */
    val dejaVuInterceptor: DejaVuInterceptor.Factory<E> = component.dejaVuInterceptorFactory()

    /**
     * Provides an observable emitting the responses metadata, for logging/stats purposes or to use
     * as an alternative to implementing the CacheMetadata.Holder interface on responses.
     */
    val cacheMetadataObservable: Observable<CacheMetadata<E>> = component.cacheMetadataObservable()

    /**
     * Returns a Single emitting current cache statistics
     */
    fun statistics() = component.statisticsCompiler()?.getStatistics()
            ?: Single.error(IllegalStateException("The cache is not using a database to persist its data"))

    companion object {

        /**
         * Use this value to provide the cache instruction as a header (this will override any existing call annotation)
         */
        const val DejaVuHeader = "DejaVuHeader"

        private fun defaultComponentProvider() = { cacheConfiguration: CacheConfiguration<Glitch> ->
            DaggerDefaultCacheComponent
                    .builder()
                    .defaultCacheModule(DefaultCacheModule(cacheConfiguration))
                    .build()
        }

        /**
         * @return Builder for CacheConfiguration
         */
        fun builder() =
                CacheConfiguration.builder(
                        GlitchFactory(),
                        defaultComponentProvider()
                )

    }
}
