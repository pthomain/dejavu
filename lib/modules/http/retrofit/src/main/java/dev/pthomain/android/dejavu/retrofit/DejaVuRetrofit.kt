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

package dev.pthomain.android.dejavu.retrofit

import android.content.Context
import dev.pthomain.android.dejavu.DejaVu
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.dejavu.retrofit.configuration.DejaVuRetrofitBuilder
import dev.pthomain.android.dejavu.retrofit.configuration.DejaVuRetrofitExtensionBuilder
import dev.pthomain.android.dejavu.retrofit.interceptors.HeaderInterceptor
import dev.pthomain.android.dejavu.error.ErrorFactory
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.utils.Logger
import dev.pthomain.android.dejavu.utils.SilentLogger
import retrofit2.CallAdapter

/**
 * Contains the Retrofit call adapter factory, DejaVuInterceptor factory,
 * and the OkHttp header interceptor for stripping cache headers.
 */
class DejaVuRetrofit<E> internal constructor(
    val callAdapterFactory: CallAdapter.Factory,
    val headerInterceptor: HeaderInterceptor,
    val interceptorFactory: DejaVuInterceptor.Factory<E>
) where E : Throwable,
      E : NetworkErrorPredicate {

    companion object {

        fun <E> extension()
            where E : Throwable,
                  E : NetworkErrorPredicate =
            DejaVuRetrofitExtensionBuilder<E>()

        /**
         * Convenience builder that creates a DejaVuRetrofit instance directly
         * from the core dependencies, without requiring a separate DejaVuBuilder step.
         *
         * @param context the Android context
         * @param errorFactory the factory for creating error instances
         * @param persistenceManagerProvider the persistence backend provider
         * @param logger the logger instance
         * @return a DejaVuRetrofitBuilder ready to build
         */
        /**
         * Convenience builder that creates a DejaVuRetrofit instance directly
         * from the core dependencies, without requiring a separate DejaVuBuilder step.
         *
         * @param context the Android context
         * @param errorFactory the factory for creating error instances
         * @param persistenceManagerProvider the persistence backend provider
         * @param logger the logger instance
         * @return a DejaVuRetrofitBuilder ready to build
         */
        fun <E> builder(
            context: Context,
            errorFactory: ErrorFactory<E>,
            persistenceManagerProvider: PersistenceManager.ComponentProvider,
            logger: Logger = SilentLogger
        ): DejaVuRetrofitBuilder<E>
            where E : Throwable,
                  E : NetworkErrorPredicate =
            DejaVu.builder(context, errorFactory, persistenceManagerProvider, logger)
                .extend(DejaVuRetrofitExtensionBuilder<E>())
                .build()
    }
}
