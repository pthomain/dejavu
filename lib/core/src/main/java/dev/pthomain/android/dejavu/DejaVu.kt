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
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.configuration.error.DejaVuGlitchFactory
import dev.pthomain.android.dejavu.di.DejaVuComponent
import dev.pthomain.android.dejavu.shared.SilentLogger
import dev.pthomain.android.dejavu.shared.persistence.PersistenceManager
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.interceptor.error.glitch.GlitchFactory

/**
 * Contains the Retrofit call adapter, DejaVuInterceptor factory and current global configuration.
 */
class DejaVu<E> internal constructor(component: DejaVuComponent<E>)
    : DejaVuComponent<E> by component
        where E : Throwable,
              E : NetworkErrorPredicate {

    companion object {

        /**
         * Use this value to provide the cache instruction as a header (this will override any existing call annotation)
         */
        const val DejaVuHeader = "DejaVuHeader"

        fun defaultBuilder(
                context: Context,
                persistenceManagerModule: PersistenceManager.ModuleProvider,
                logger: Logger = SilentLogger
        ) = builder(
                context,
                DejaVuGlitchFactory(GlitchFactory()),
                persistenceManagerModule,
                logger
        )

        fun <E> builder(
                context: Context,
                errorFactory: ErrorFactory<E>,
                persistenceManagerModule: PersistenceManager.ModuleProvider,
                logger: Logger = SilentLogger
        ) where E : Throwable,
                E : NetworkErrorPredicate =
                DejaVuBuilder(
                        context.applicationContext,
                        logger,
                        errorFactory,
                        persistenceManagerModule
                )
    }

}