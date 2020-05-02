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

import com.google.gson.Gson
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.demo.DemoActivity
import dev.pthomain.android.dejavu.demo.presenter.BaseDemoPresenter
import dev.pthomain.android.dejavu.demo.presenter.retrofit.clients.ObservableClients
import dev.pthomain.android.dejavu.demo.presenter.retrofit.clients.SingleClients
import dev.pthomain.android.dejavu.demo.presenter.retrofit.clients.adapters.SingleDataClientAdapter
import dev.pthomain.android.dejavu.demo.presenter.retrofit.clients.adapters.SingleOperationsClientAdapter
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal abstract class BaseRetrofitDemoPresenter(
        demoActivity: DemoActivity,
        uiLogger: Logger
) : BaseDemoPresenter(demoActivity, uiLogger) {

    private fun retrofit() =
            Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getOkHttpClient(uiLogger))
                    .addConverterFactory(GsonConverterFactory.create(Gson()))
                    .addCallAdapterFactory(dejaVu.callAdapterFactory)
                    .build()

    private fun getOkHttpClient(logger: Logger) = OkHttpClient.Builder().let {
        it.addInterceptor(getHttpLoggingInterceptor(logger))
        it.followRedirects(true)
        it.build()
    }

    private fun getHttpLoggingInterceptor(logger: Logger) =
            HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    logger.d(this, message)
                }
            }).apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

    protected fun dataClient() = with(retrofit()) {
        if (useSingle)
            SingleDataClientAdapter(create(SingleClients.Data::class.java))
        else
            create(ObservableClients.Data::class.java)
    }

    protected fun operationsClient() = with(retrofit()) {
        if (useSingle)
            SingleOperationsClientAdapter(create(SingleClients.Operations::class.java))
        else
            create(ObservableClients.Operations::class.java)
    }

}