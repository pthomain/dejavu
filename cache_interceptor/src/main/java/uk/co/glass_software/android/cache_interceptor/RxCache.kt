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

package uk.co.glass_software.android.cache_interceptor

import uk.co.glass_software.android.cache_interceptor.configuration.CacheConfiguration
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.injection.CacheComponent
import uk.co.glass_software.android.cache_interceptor.injection.DaggerDefaultCacheComponent
import uk.co.glass_software.android.cache_interceptor.injection.DefaultConfigurationModule
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiErrorFactory
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory

open class RxCache<E> internal constructor(component: CacheComponent<E>)
        where E : Exception,
              E : NetworkErrorProvider {

    val configuration: CacheConfiguration<E> = component.configuration()
    val retrofitCacheAdapterFactory: RetrofitCacheAdapterFactory<E> = component.retrofitCacheAdapterFactory()
    val rxCacheInterceptor: RxCacheInterceptor.Factory<E> = component.rxCacheInterceptorFactory()

    companion object {

        //Use this value to provide the cache instruction as a header
        const val RxCacheHeader = "RxCacheHeader"

        private fun defaultComponentProvider() = { cacheConfiguration: CacheConfiguration<ApiError> ->
            DaggerDefaultCacheComponent
                    .builder()
                    .defaultConfigurationModule(DefaultConfigurationModule(cacheConfiguration))
                    .build()
        }

        fun builder() = CacheConfiguration.builder(
                ApiErrorFactory(),
                defaultComponentProvider()
        )
    }
}
