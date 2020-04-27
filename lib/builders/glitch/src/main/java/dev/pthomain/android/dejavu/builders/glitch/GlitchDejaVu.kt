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

package dev.pthomain.android.dejavu.builders.glitch

import android.content.Context
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.CacheModule
import dev.pthomain.android.dejavu.cache.TransientResponse
import dev.pthomain.android.dejavu.configuration.DejaVuBuilder
import dev.pthomain.android.dejavu.configuration.error.DejaVuGlitchFactory
import dev.pthomain.android.dejavu.di.DejaVuComponent
import dev.pthomain.android.dejavu.di.DejaVuModule
import dev.pthomain.android.dejavu.interceptors.InterceptorModule
import dev.pthomain.android.dejavu.persistence.statistics.StatisticsModule
import dev.pthomain.android.dejavu.retrofit.RetrofitModule
import dev.pthomain.android.dejavu.shared.PersistenceManager
import dev.pthomain.android.dejavu.shared.di.SharedModule
import dev.pthomain.android.dejavu.shared.token.instruction.RequestMetadata
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.glitch.Glitch
import dev.pthomain.android.glitchy.interceptor.error.glitch.GlitchFactory
import javax.inject.Singleton


object GlitchDejaVu {

    class Builder(
            context: Context,
            persistenceManager: PersistenceManager,
            errorFactory: ErrorFactory<Glitch> = DejaVuGlitchFactory(GlitchFactory())
    ) : DejaVuBuilder<Glitch>(
            context,
            errorFactory,
            persistenceManager
    ) {

        override fun componentProvider(
                context: Context,
                logger: Logger,
                errorFactory: ErrorFactory<Glitch>,
                persistenceManager: PersistenceManager,
                operationPredicate: (metadata: RequestMetadata<*>) -> Operation.Remote?,
                durationPredicate: (TransientResponse<*>) -> Int?
        ): DejaVuComponent<Glitch> =
                DaggerGlitchDejaVu_Component.builder()
                        .sharedModule(SharedModule(context, logger))
                        .glitchCacheModule(GlitchCacheModule(persistenceManager))
                        .module(Module(
                                errorFactory,
                                operationPredicate,
                                durationPredicate
                        ))
                        .build()

    }

    @Singleton
    @dagger.Component(modules = [Module::class])
    internal interface Component : DejaVuComponent<Glitch>

    @dagger.Module(includes = [
        GlitchStatisticsModule::class,
        GlitchInterceptorModule::class,
        GlitchCacheModule::class,
        GlitchRetrofitModule::class
    ])
    internal class Module(
            errorFactory: ErrorFactory<Glitch>,
            operationPredicate: (metadata: RequestMetadata<*>) -> Operation.Remote?,
            durationPredicate: (TransientResponse<*>) -> Int?
    ) : DejaVuModule<Glitch>(
            errorFactory,
            operationPredicate,
            durationPredicate
    )

    @dagger.Module
    internal class GlitchStatisticsModule : StatisticsModule()

    @dagger.Module
    internal class GlitchInterceptorModule : InterceptorModule<Glitch>()

    @dagger.Module
    internal class GlitchCacheModule(persistenceManager: PersistenceManager) : CacheModule<Glitch>(persistenceManager)

    @dagger.Module
    internal class GlitchRetrofitModule : RetrofitModule<Glitch>()

}