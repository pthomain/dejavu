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

package dev.pthomain.android.dejavu.injection.component

import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.configuration.NetworkErrorProvider
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.statistics.StatisticsCompiler
import dev.pthomain.android.dejavu.response.CacheMetadata
import dev.pthomain.android.dejavu.retrofit.RetrofitCallAdapterFactory
import io.reactivex.Observable

internal interface CacheComponent<E>
        where E : Exception,
              E : NetworkErrorProvider {

    fun configuration(): CacheConfiguration<E>
    fun dejaVuInterceptorFactory(): DejaVuInterceptor.Factory<E>
    fun retrofitCacheAdapterFactory(): RetrofitCallAdapterFactory<E>
    fun cacheMetadataObservable(): Observable<CacheMetadata<E>>
    fun statisticsCompiler(): StatisticsCompiler?

}
