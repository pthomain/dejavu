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

package dev.pthomain.android.dejavu.demo.presenter.retrofit

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.demo.DemoActivity
import dev.pthomain.android.dejavu.demo.dejavu.DejaVuRetrofitClient
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.ErrorFactoryType
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.ErrorFactoryType.Custom
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.ErrorFactoryType.Default
import dev.pthomain.android.dejavu.demo.dejavu.clients.retrofit.RetrofitObservableClients
import dev.pthomain.android.dejavu.demo.dejavu.clients.retrofit.RetrofitSingleClients
import dev.pthomain.android.dejavu.demo.dejavu.error.CustomApiError
import dev.pthomain.android.dejavu.demo.presenter.base.BaseDemoPresenter
import dev.pthomain.android.glitchy.core.interceptor.error.glitch.Glitch

internal abstract class BaseRetrofitDemoPresenter(
        demoActivity: DemoActivity,
        uiLogger: Logger
) : BaseDemoPresenter<RetrofitSingleClients.Operations, RetrofitObservableClients.Operations, DejaVuRetrofitClient>(
        demoActivity,
        uiLogger
) {

    override fun newClient() = when (errorFactoryType) {
        Default -> dejaVuFactory.createRetrofit(errorFactoryType as ErrorFactoryType<Glitch>)
        Custom -> dejaVuFactory.createRetrofit(errorFactoryType as ErrorFactoryType<CustomApiError>)
    }

}