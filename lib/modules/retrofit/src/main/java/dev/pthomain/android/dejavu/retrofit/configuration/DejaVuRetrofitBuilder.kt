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

package dev.pthomain.android.dejavu.retrofit.configuration

import dev.pthomain.android.dejavu.configuration.ExtensionBuilder
import dev.pthomain.android.dejavu.retrofit.DejaVuRetrofit
import dev.pthomain.android.dejavu.retrofit.di.DejaVuRetrofitModule
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

class DejaVuRetrofitBuilder<E> internal constructor() : ExtensionBuilder<DejaVuRetrofitBuilder<E>>
        where E : Throwable,
              E : NetworkErrorPredicate {

    private var parentModules: List<Module>? = null

    override fun accept(modules: List<Module>) = apply {
        parentModules = modules
    }

    /**
     * Returns an instance of DejaVu.
     */
    fun build(): DejaVuRetrofit<E> {
        val parentModules = this.parentModules
                ?: throw IllegalStateException("This builder needs to call DejaVuBuilder::extend")

        return koinApplication {
            modules(parentModules + DejaVuRetrofitModule<E>().module)
        }.koin.run {
            DejaVuRetrofit(get(), get())
        }
    }
}