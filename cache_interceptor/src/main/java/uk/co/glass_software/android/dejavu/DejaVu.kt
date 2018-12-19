/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.dejavu

import io.reactivex.Observable
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.injection.component.CacheComponent
import uk.co.glass_software.android.dejavu.injection.component.DaggerDefaultCacheComponent
import uk.co.glass_software.android.dejavu.injection.module.DefaultCacheModule
import uk.co.glass_software.android.dejavu.interceptors.DejaVuInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.interceptors.internal.error.GlitchFactory
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.retrofit.RetrofitCallAdapterFactory

/**
 * Contains the Retrofit call adapter, DejaVuInterceptor factory and current global configuration.
 */
class DejaVu<E> internal constructor(component: CacheComponent<E>)
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

    companion object {

        /**
         * Use this value to provide the cache instruction as a header
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
