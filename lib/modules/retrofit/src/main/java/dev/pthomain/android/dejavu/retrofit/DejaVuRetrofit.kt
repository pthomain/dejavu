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
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.DejaVu
import dev.pthomain.android.dejavu.configuration.DejaVuBuilder
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.dejavu.retrofit.configuration.DejaVuRetrofitBuilder
import dev.pthomain.android.dejavu.utils.SilentLogger
import dev.pthomain.android.glitchy.core.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import retrofit2.CallAdapter

/**
 * Contains the Retrofit call adapter, DejaVuInterceptor factory and current global configuration.
 */
class DejaVuRetrofit<E> internal constructor(
        val callAdapterFactory: CallAdapter.Factory
) where E : Throwable,
        E : NetworkErrorPredicate {

    companion object {

        fun <E> extension()
                where E : Throwable,
                      E : NetworkErrorPredicate =
                DejaVuRetrofitBuilder<E>()

        fun <E> builder(dejaVuBuilder: DejaVuBuilder<E>)
                where E : Throwable,
                      E : NetworkErrorPredicate =
                dejaVuBuilder.extend(extension<E>())

        fun <E> builder(
                context: Context,
                errorFactory: ErrorFactory<E>,
                persistenceManagerModule: PersistenceManager.ModuleProvider,
                logger: Logger = SilentLogger
        ) where E : Throwable,
                E : NetworkErrorPredicate =
                builder(
                        DejaVu.builder(
                                context,
                                errorFactory,
                                persistenceManagerModule,
                                logger
                        )
                )
    }

}