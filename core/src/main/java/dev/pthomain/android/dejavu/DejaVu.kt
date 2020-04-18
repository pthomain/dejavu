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
import dev.pthomain.android.dejavu.builders.configuration.ConfigurationBuilder
import dev.pthomain.android.dejavu.cache.TransientResponse
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.RequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.di.DejaVuComponent
import dev.pthomain.android.dejavu.di.glitch.DaggerGlitchDejaVuComponent
import dev.pthomain.android.dejavu.di.glitch.GlitchDejaVuModule
import dev.pthomain.android.dejavu.error.DejaVuGlitchFactory
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.dejavu.persistence.PersistenceManagerFactory
import dev.pthomain.android.dejavu.serialisation.Serialiser
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.interceptor.error.glitch.GlitchFactory
import dev.pthomain.android.mumbo.base.EncryptionManager

/**
 * Contains the Retrofit call adapter, DejaVuInterceptor factory and current global configuration.
 */
class DejaVu<E> internal constructor(
        private val component: DejaVuComponent<E>
) : DejaVuComponent<E> by component
        where E : Throwable,
              E : NetworkErrorPredicate {

    companion object {
        /**
         * Use this value to provide the cache instruction as a header (this will override any existing call annotation)
         */
        const val DejaVuHeader = "DejaVuHeader" //TODO find a way to strip this after processing

        /**
         * @return Builder for DejaVu.Configuration
         */
        fun <E> builder(context: Context,
                        serialiser: Serialiser,
                        errorFactory: ErrorFactory<E>,
                        componentProvider: (Configuration<E>) -> DejaVuComponent<E>)
                where E : Throwable,
                      E : NetworkErrorPredicate = ConfigurationBuilder(
                errorFactory,
                context,
                serialiser,
                componentProvider
        )

        /**
         * @return Builder for DejaVu.Configuration
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

    /**
     * Class holding the global cache configuration. Values defined here are used by default
     * but (for some of them) can be overridden on a per-call basis via the use of arguments
     * passed to the call's CacheInstruction (and by extension in the Retrofit call's annotation).
     *
     * @see dev.pthomain.android.dejavu.interceptors.cache.instruction.CacheInstruction
     * @see dev.pthomain.android.dejavu.retrofit.annotations.Cache
     * @see dev.pthomain.android.dejavu.retrofit.annotations.DoNotCache
     * @see dev.pthomain.android.dejavu.retrofit.annotations.Invalidate
     * @see dev.pthomain.android.dejavu.retrofit.annotations.Clear
     *
     * TODO update JavaDoc
     */
    data class Configuration<E> internal constructor(
            internal val context: Context,
            internal val logger: Logger,
            internal val errorFactory: ErrorFactory<E>,
            internal val serialiser: Serialiser,
            internal val encryptionManager: EncryptionManager?,
            internal val useDatabase: Boolean,
            internal val persistenceManagerPicker: ((PersistenceManagerFactory<E>) -> PersistenceManager<E>)?,
            internal val operationPredicate: (metadata: RequestMetadata<*>) -> Operation.Remote?,
            internal val durationPredicate: (TransientResponse<*>) -> Int?
    ) where E : Throwable,
            E : NetworkErrorPredicate

}