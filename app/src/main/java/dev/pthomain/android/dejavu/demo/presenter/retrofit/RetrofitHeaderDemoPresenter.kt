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
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority.FreshnessPriority
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority.NetworkPriority.OFFLINE
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation.Local.Clear
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation.Local.Invalidate
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.response.DejaVuResult

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
            )).map { (it as DejaVuResult.Response).response }

    override fun getOfflineSingle(preference: FreshnessPriority) =
            executeOperation(
                    Cache(priority = CachePriority.with(OFFLINE, preference))
            ).map { (it as DejaVuResult.Response).response }.firstOrError()

    override fun getClearEntriesCompletable() =
            executeOperation(Clear())

    override fun getInvalidateCompletable() =
            executeOperation(Invalidate())

    private fun executeOperation(cacheOperation: Operation) =
            catFactClient().execute(cacheOperation)

}
