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

package dev.pthomain.android.dejavu.di

import dev.pthomain.android.dejavu.DejaVu.Configuration
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.interceptors.HeaderInterceptor
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import retrofit2.CallAdapter

interface DejaVuComponent<E>
        where E : Throwable,
              E : NetworkErrorPredicate {

    /**
     * Provides the current configuration.
     */
    fun configuration(): Configuration<E>

    /**
     * Provides a generic DejaVuInterceptor factory to use with any Observable / Single / CacheOperation.
     * @see dev.pthomain.android.dejavu.interceptors.RxType
     */
    fun dejaVuInterceptorFactory(): DejaVuInterceptor.Factory<E>

    /**
     * Provides the OkHttp header interceptor to add to your OkHttp setup if you plan
     * to use header instructions.
     *
     * NB: Header instructions would work without this interceptor but the header would be sent
     * alongside the request to the API server. This might not be an issue, but if it is,
     * this interceptor will remove the header before the network call is made.
     */
    fun headerInterceptor(): HeaderInterceptor

    /**
     * Provides the adapter factory to use with Retrofit.
     */
    fun retrofitCallAdapterFactory(): CallAdapter.Factory

    /**
     * Provides statistics about the content of the cache.
     */
    //FIXME
//    fun statisticsCompiler(): dev.pthomain.android.dejavu.persistence.statistics.StatisticsCompiler

}
