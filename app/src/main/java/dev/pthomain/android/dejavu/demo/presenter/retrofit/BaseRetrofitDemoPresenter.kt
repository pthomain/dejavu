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

package dev.pthomain.android.dejavu.demo.presenter.retrofit

import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.demo.DemoActivity
import dev.pthomain.android.dejavu.demo.presenter.BaseDemoPresenter
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal abstract class BaseRetrofitDemoPresenter(demoActivity: DemoActivity,
                                                  uiLogger: Logger)
    : BaseDemoPresenter(demoActivity, uiLogger) {

    private fun retrofit() =
            Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getOkHttpClient(uiLogger))
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(dejaVu.retrofitCallAdapterFactory)
                    .build()

    private fun getOkHttpClient(logger: Logger) = OkHttpClient.Builder().let {
        it.addInterceptor(getHttpLoggingInterceptor(logger))
        it.followRedirects(true)
        it.build()
    }

    private fun getHttpLoggingInterceptor(logger: Logger) =
            HttpLoggingInterceptor { logger.d(this, it) }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

    protected fun catFactClient() =
            if (useSingle) SingleClientWrapper(retrofit().create(SingleCatFactClient::class.java))
            else retrofit().create(ObservableCatFactClient::class.java)

    private class SingleClientWrapper(private val singleClient: SingleCatFactClient) : ObservableCatFactClient {
        override fun get() = singleClient.get().toObservable()
        override fun compressed() = singleClient.compressed().toObservable()
        override fun encrypted() = singleClient.encrypted().toObservable()
        override fun compressedEncrypted() = singleClient.compressedEncrypted().toObservable()
        override fun freshOnly() = singleClient.freshOnly().toObservable()
        override fun freshOnlyCompressed() = singleClient.freshOnlyCompressed().toObservable()
        override fun freshOnlyEncrypted() = singleClient.freshOnlyEncrypted().toObservable()
        override fun freshOnlyCompressedEncrypted() = singleClient.freshOnlyCompressedEncrypted().toObservable()
        override fun refresh() = singleClient.refresh().toObservable()
        override fun refreshFreshOnly() = singleClient.refreshFreshOnly().toObservable()
        override fun clearCache() = singleClient.clearCache()
        override fun invalidate() = singleClient.invalidate()
        override fun offline() = singleClient.offline()
        override fun offlineFreshOnly() = singleClient.offlineFreshOnly()
        override fun execute(operation: Operation) = singleClient.execute(operation)
    }
}