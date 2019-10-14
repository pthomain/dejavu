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

package dev.pthomain.android.dejavu.injection.module

import android.net.Uri
import dagger.Module
import dagger.Provides
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.configuration.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.interceptors.InterceptorModule
import dev.pthomain.android.dejavu.interceptors.cache.CacheModule
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceModule
import dev.pthomain.android.dejavu.interceptors.cache.persistence.statistics.StatisticsModule
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationModule
import dev.pthomain.android.dejavu.retrofit.RetrofitModule
import javax.inject.Singleton

@Module(includes = [
    SerialisationModule::class,
    PersistenceModule::class,
    StatisticsModule::class,
    InterceptorModule::class,
    CacheModule::class,
    RetrofitModule::class
])
internal abstract class BaseDejaVuModule<E>(
        protected val configuration: DejaVuConfiguration<E>
) : DejaVuModule
        where E : Exception,
              E : NetworkErrorPredicate {

    companion object {
        const val DATABASE_NAME = "dejavu.db"
        const val DATABASE_VERSION = 1
    }

    @Provides
    @Singleton
    fun provideContext() =
            configuration.context

    @Provides
    @Singleton
    fun provideConfiguration() =
            configuration

    @Provides
    @Singleton
    fun provideLogger() =
            configuration.logger

    @Provides
    @Singleton
    fun provideUriParser() = object : Function1<String, Uri> {
        override fun get(t1: String) = Uri.parse(t1)
    }

}
