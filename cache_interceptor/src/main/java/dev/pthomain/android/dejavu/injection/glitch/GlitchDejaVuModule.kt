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

package dev.pthomain.android.dejavu.injection.glitch

import dagger.Module
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.injection.DejaVuModule
import dev.pthomain.android.dejavu.injection.ProdModule
import dev.pthomain.android.dejavu.interceptors.InterceptorModule
import dev.pthomain.android.dejavu.interceptors.cache.CacheModule
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceModule
import dev.pthomain.android.dejavu.interceptors.cache.persistence.statistics.StatisticsModule
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationModule
import dev.pthomain.android.dejavu.retrofit.RetrofitModule
import dev.pthomain.android.glitchy.interceptor.error.glitch.Glitch

@Module(includes = [
    GlitchProdModule::class,
    GlitchSerialisationModule::class,
    GlitchPersistenceModule::class,
    GlitchStatisticsModule::class,
    GlitchInterceptorModule::class,
    GlitchCacheModule::class,
    GlitchRetrofitModule::class
])
internal class GlitchDejaVuModule(configuration: DejaVuConfiguration<Glitch>)
    : DejaVuModule<Glitch>(configuration)

@Module
internal class GlitchProdModule : ProdModule<Glitch>()

@Module
internal class GlitchSerialisationModule : SerialisationModule<Glitch>()

@Module
internal class GlitchPersistenceModule : PersistenceModule<Glitch>()

@Module
internal class GlitchStatisticsModule : StatisticsModule<Glitch>()

@Module
internal class GlitchInterceptorModule : InterceptorModule<Glitch>()

@Module
internal class GlitchCacheModule : CacheModule<Glitch>()

@Module
internal class GlitchRetrofitModule : RetrofitModule<Glitch>()