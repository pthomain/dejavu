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

package dev.pthomain.android.dejavu.builders.glitch.di

import android.content.Context
import dagger.Module
import dev.pthomain.android.dejavu.cache.CacheModule
import dev.pthomain.android.dejavu.cache.TransientResponse
import dev.pthomain.android.dejavu.di.DejaVuModule
import dev.pthomain.android.dejavu.interceptors.InterceptorModule
import dev.pthomain.android.dejavu.persistence.statistics.StatisticsModule
import dev.pthomain.android.dejavu.retrofit.RetrofitModule
import dev.pthomain.android.dejavu.shared.PersistenceManager
import dev.pthomain.android.dejavu.shared.token.instruction.RequestMetadata
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation
import dev.pthomain.android.glitchy.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.interceptor.error.glitch.Glitch

@Module(includes = [
    GlitchStatisticsModule::class,
    GlitchInterceptorModule::class,
    GlitchCacheModule::class,
    GlitchRetrofitModule::class
])
internal class GlitchDejaVuModule(
        context: Context,
        errorFactory: ErrorFactory<Glitch>,
        operationPredicate: (metadata: RequestMetadata<*>) -> Operation.Remote?,
        durationPredicate: (TransientResponse<*>) -> Int?
) : DejaVuModule<Glitch>(
        context,
        errorFactory,
        operationPredicate,
        durationPredicate
)

@Module
internal class GlitchStatisticsModule : StatisticsModule()

@Module
internal class GlitchInterceptorModule : InterceptorModule<Glitch>()

@Module
internal class GlitchCacheModule(persistenceManager: PersistenceManager) : CacheModule<Glitch>(persistenceManager)

@Module
internal class GlitchRetrofitModule : RetrofitModule<Glitch>()