/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.cache_interceptor.demo.presenter.retrofit

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity
import uk.co.glass_software.android.cache_interceptor.demo.presenter.BaseDemoPresenter

internal abstract class BaseRetrofitDemoPresenter(demoActivity: DemoActivity,
                                                  uiLogger: Logger)
    : BaseDemoPresenter(demoActivity, uiLogger) {

    protected val retrofit = Retrofit.Builder()
            .baseUrl(BaseDemoPresenter.BASE_URL)
            .client(getOkHttpClient(uiLogger))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(rxCache.retrofitCacheAdapterFactory)
            .build()

    private fun getOkHttpClient(logger: Logger) = OkHttpClient.Builder().let {
        it.addInterceptor(getHttpLoggingInterceptor(logger))
        it.followRedirects(true)
        it.build()
    }

    private fun getHttpLoggingInterceptor(logger: Logger) =
            HttpLoggingInterceptor { logger.d(it) }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

    protected val catFactClient = retrofit.create(CatFactClient::class.java)

}