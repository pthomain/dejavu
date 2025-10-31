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

package dev.pthomain.android.dejavu.configuration

import android.content.Context
import dev.pthomain.android.boilerplate.core.builder.Extendable
import dev.pthomain.android.boilerplate.core.builder.ExtensionBuilder
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.DejaVu
import dev.pthomain.android.dejavu.cache.metadata.response.TransientResponse
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.RequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.configuration.OperationPredicate.Inactive
import dev.pthomain.android.dejavu.di.DejaVuModule
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.glitchy.core.interceptor.interceptors.error.ErrorFactory

import dev.pthomain.android.glitchy.core.interceptor.interceptors.error.NetworkErrorPredicate
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

class DejaVuBuilder<E> internal constructor(
        private val context: Context,
        private val logger: Logger,
        private val errorFactory: ErrorFactory<E>,
        private val persistenceManagerModule: PersistenceManager.ModuleProvider,
) : Extendable<Module>
        where E : Throwable,
              E : NetworkErrorPredicate {

    private var operationMapper: (RequestMetadata<*>) -> Remote? = Inactive
    private var durationMapper: (TransientResponse<*>) -> Int? = { null }

    /**
     * Sets a mapper for ad-hoc response caching. This mapper will be called for every
     * request and will always take precedence on the provided request operation.
     *
     * It will be called with the target response class and associated request metadata before
     * the call is made in order to establish the operation to associate with that request.
     * @see Remote //TODO explain remote vs local
     *
     * Returning null means the cache will instead take into account the directives defined
     * as annotation or header on that request (if present).
     *
     * To cache all response with default CACHE operation and expiration period, use CachePredicate.CacheAll.
     * To disable any caching, use CachePredicate.CacheNone.
     *
     * Otherwise, you can implement your own mapper to return the appropriate operation base on
     * the given RequestMetadata for the request being made.
     */
    fun operationMapper(operationMapper: (metadata: RequestMetadata<*>) -> Remote?) =
            apply { this.operationMapper = operationMapper }

    /**
     * Sets a mapper for ad-hoc response duration caching.
     *
     * If not null, the value returned by this mapper will take precedence on the
     * cache duration provided in the request's operation.
     *
     * This is useful for responses containing cache duration information, enabling server-side
     * cache control.
     */
    fun durationMapper(durationMapper: (TransientResponse<*>) -> Int?) =
            apply { this.durationMapper = durationMapper }

    override fun <B, EB : ExtensionBuilder<B, Module, EB>> extend(extensionBuilder: EB) =
            extensionBuilder.accept(modules())

    private fun modules() = DejaVuModule(
            context.applicationContext,
            logger,
            errorFactory,
            persistenceManagerModule,
            operationMapper,
            durationMapper
    ).modules

    /**
     * Returns an instance of DejaVu.
     */
    fun build(): DejaVu<E> {
        val koin = koinApplication {
            modules(this@DejaVuBuilder.modules())
        }.koin

        return DejaVu(koin.get())
    }
}
