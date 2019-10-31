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
import dev.pthomain.android.dejavu.demo.DemoActivity
import dev.pthomain.android.dejavu.demo.model.CatFactResponse
import dev.pthomain.android.dejavu.demo.presenter.BaseDemoPresenter
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority.CacheMode
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority.CachePreference
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation.*
import dev.pthomain.android.dejavu.interceptors.cache.metadata.RequestMetadata
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch

internal class VolleyDemoPresenter(demoActivity: DemoActivity,
                                   uiLogger: Logger)
    : BaseDemoPresenter(demoActivity, uiLogger) {

    private val requestQueue: RequestQueue = Volley.newRequestQueue(demoActivity)
    private val responseClass = CatFactResponse::class.java

    companion object {
        private const val URL = BASE_URL + ENDPOINT
    }

    override fun getResponseObservable(cachePriority: CachePriority,
                                       encrypt: Boolean,
                                       compress: Boolean) =
            newObservable(Cache(priority = cachePriority))

    private fun newObservable(operation: Operation) =
            with(RequestMetadata.Plain(responseClass, URL)) {
                VolleyObservable.observable<CatFactResponse, Glitch>(
                        requestQueue,
                        gson,
                        dejaVu.dejaVuInterceptorFactory.create(
                                operation,
                                this
                        ),
                        this
                )
            }

    private fun newCacheOperation(operation: Operation) =
            with(RequestMetadata.Plain(responseClass, URL)) {
                VolleyObservable.cacheOperation<CatFactResponse, Glitch>(
                        requestQueue,
                        gson,
                        dejaVu.dejaVuInterceptorFactory.create(
                                operation,
                                this
                        ),
                        this
                )
            }

    override fun getOfflineSingle(preference: CachePreference) =
            getResponseObservable(
                    CachePriority.with(CacheMode.OFFLINE, preference),
                    false,
                    false
            ).firstOrError()

    override fun getClearEntriesCompletable() =
            newCacheOperation(Clear())

    override fun getInvalidateCompletable() =
            newCacheOperation(Invalidate())

}