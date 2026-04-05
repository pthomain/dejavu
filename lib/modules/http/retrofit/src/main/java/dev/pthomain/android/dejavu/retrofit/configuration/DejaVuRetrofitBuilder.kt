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
import dev.pthomain.android.dejavu.di.DejaVuComponent
import dev.pthomain.android.dejavu.retrofit.DejaVuRetrofit
import dev.pthomain.android.dejavu.retrofit.di.DejaVuRetrofitComponent
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate

class DejaVuRetrofitBuilder<E> internal constructor()
    : ExtensionBuilder<DejaVuRetrofitBuilder<E>, DejaVuRetrofit<E>, E>
        where E : Throwable,
              E : NetworkErrorPredicate {

    private var parentComponent: DejaVuComponent<E>? = null

    override fun accept(component: DejaVuComponent<E>) = apply {
        parentComponent = component
    }

    /**
     * Returns an instance of DejaVuRetrofit.
     */
    override fun build(): DejaVuRetrofit<E> {
        val component = this.parentComponent
                ?: throw IllegalStateException("This builder needs to call DejaVuBuilder::extend")

        val retrofitComponent = DejaVuRetrofitComponent(component)

        return DejaVuRetrofit(
                retrofitComponent.callAdapterFactory,
                component.interceptorFactory
        )
    }
}
