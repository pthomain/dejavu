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
import dev.pthomain.android.boilerplate.core.utils.rx.observable
import dev.pthomain.android.boilerplate.core.utils.rx.single
import dev.pthomain.android.dejavu.demo.DemoActivity
import dev.pthomain.android.dejavu.demo.model.CatFactResponse
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.CachePriority
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.CachePriority.FreshnessPriority
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.CachePriority.NetworkPriority.LOCAL_ONLY
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local.Invalidate
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.response.Empty
import dev.pthomain.android.dejavu.interceptors.response.Response
import dev.pthomain.android.dejavu.interceptors.response.Result
import io.reactivex.Observable
import io.reactivex.Single

internal class RetrofitHeaderDemoPresenter(demoActivity: DemoActivity,
                                           uiLogger: Logger)
    : BaseRetrofitDemoPresenter(demoActivity, uiLogger) {

    override fun getResponseObservable(cachePriority: CachePriority,
                                       encrypt: Boolean,
                                       compress: Boolean) =
            executeOperation(Cache(
                    priority = cachePriority,
                    encrypt = encrypt,
                    compress = compress
            )).flatMap {
                when (it) {
                    is Response<*, *> -> (it as Response<CatFactResponse, *>).response.observable()
                    is Empty<*, *> -> Observable.error(it.exception)
                    is Result<*> -> Observable.empty()
                }
            }

    override fun getOfflineSingle(freshness: FreshnessPriority) =
            executeOperation(
                    Cache(priority = CachePriority.with(LOCAL_ONLY, freshness))
            ).firstOrError().flatMap {
                when (it) {
                    is Response<*, *> -> (it as Response<CatFactResponse, *>).response.single()
                    is Empty<*, *> -> Single.error(it.exception)
                    is Result<*> -> Single.error(NoSuchElementException("This operation does not emit any response: ${it.cacheToken.instruction.operation.type}"))
                }
            }

    override fun getClearEntriesCompletable() =
            executeOperation(Clear())

    override fun getInvalidateCompletable() =
            executeOperation(Invalidate)

    private fun executeOperation(cacheOperation: Operation) =
            catFactClient().execute(cacheOperation)

}
