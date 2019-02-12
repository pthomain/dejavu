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

package uk.co.glass_software.android.dejavu.demo.presenter.volley

import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import uk.co.glass_software.android.boilerplate.Boilerplate.context
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Clear
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.*
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Invalidate
import uk.co.glass_software.android.dejavu.demo.DemoActivity
import uk.co.glass_software.android.dejavu.demo.model.CatFactResponse
import uk.co.glass_software.android.dejavu.demo.presenter.BaseDemoPresenter

internal class VolleyDemoPresenter(demoActivity: DemoActivity,
                                   uiLogger: Logger)
    : BaseDemoPresenter(demoActivity, uiLogger) {

    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)
    private val responseClass = CatFactResponse::class.java

    companion object {
        private const val URL = BaseDemoPresenter.BASE_URL + BaseDemoPresenter.ENDPOINT
    }

    override fun getResponseObservable(isRefresh: Boolean,
                                       encrypt: Boolean,
                                       compress: Boolean,
                                       freshOnly: Boolean) =
            getObservableForOperation(when {
                isRefresh -> Refresh(freshOnly = freshOnly)
                else -> Cache(
                        encrypt = encrypt,
                        compress = compress,
                        freshOnly = freshOnly
                )
            })

    private fun getObservableForOperation(cacheOperation: Operation) =
            VolleyObservable.createDefault(
                    requestQueue,
                    gson,
                    responseClass,
                    dejaVu.dejaVuInterceptor.create(
                            CacheInstruction(responseClass, cacheOperation),
                            URL
                    ),
                    URL
            )

    override fun getOfflineSingle(freshOnly: Boolean) =
            getObservableForOperation(Offline(freshOnly)).firstOrError()!!

    override fun getClearEntriesCompletable() =
            getObservableForOperation(Clear()).ignoreElements()!!

    override fun getInvalidateCompletable() =
            getObservableForOperation(Invalidate).ignoreElements()!!

}