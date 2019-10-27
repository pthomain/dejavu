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

package dev.pthomain.android.dejavu.demo.presenter.volley

import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.configuration.instruction.Operation
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Clear
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Expiring.*
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Invalidate
import dev.pthomain.android.dejavu.demo.DemoActivity
import dev.pthomain.android.dejavu.demo.model.CatFactResponse
import dev.pthomain.android.dejavu.demo.presenter.BaseDemoPresenter
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import io.reactivex.Observable

internal class VolleyDemoPresenter(demoActivity: DemoActivity,
                                   uiLogger: Logger)
    : BaseDemoPresenter(demoActivity, uiLogger) {

    private val requestQueue: RequestQueue = Volley.newRequestQueue(demoActivity)
    private val responseClass = CatFactResponse::class.java

    companion object {
        private const val URL = BASE_URL + ENDPOINT
    }

    override fun getResponseObservable(isRefresh: Boolean,
                                       encrypt: Boolean,
                                       compress: Boolean,
                                       freshOnly: Boolean): Observable<CatFactResponse> =
            getObservableForOperation(when {
                isRefresh -> Refresh(freshOnly = freshOnly)
                else -> Cache(
                        encrypt = encrypt,
                        compress = compress,
                        freshOnly = freshOnly
                )
            })

    private fun getObservableForOperation(cacheOperation: Operation): Observable<CatFactResponse> =
            RequestMetadata.Plain(responseClass, URL).let {
                VolleyObservable.createDefault(
                        requestQueue,
                        gson,
                        dejaVu.dejaVuInterceptor.create(
                                cacheOperation,
                                it
                        ),
                        it
                )
            }

    override fun getOfflineSingle(freshOnly: Boolean) =
            getObservableForOperation(Offline(freshOnly)).firstOrError()!!

    override fun getClearEntriesCompletable() =
            getObservableForOperation(Clear()).ignoreElements()!!

    override fun getInvalidateCompletable() =
            getObservableForOperation(Invalidate()).ignoreElements()!!

}