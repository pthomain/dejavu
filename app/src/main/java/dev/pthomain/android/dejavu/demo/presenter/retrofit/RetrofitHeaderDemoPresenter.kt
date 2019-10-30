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
import dev.pthomain.android.dejavu.configuration.instruction.CachePriority
import dev.pthomain.android.dejavu.configuration.instruction.CachePriority.CacheMode
import dev.pthomain.android.dejavu.configuration.instruction.CachePriority.CachePreference
import dev.pthomain.android.dejavu.configuration.instruction.Operation
import dev.pthomain.android.dejavu.configuration.instruction.Operation.*
import dev.pthomain.android.dejavu.demo.DemoActivity
import dev.pthomain.android.dejavu.demo.model.CatFactResponse

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
            )).map { it.response as CatFactResponse }

    override fun getOfflineSingle(preference: CachePreference) =
            executeOperation(
                    Cache(priority = CachePriority.with(CacheMode.OFFLINE, preference))
            )

    override fun getClearEntriesCompletable() =
            executeOperation(Clear())

    override fun getInvalidateCompletable() =
            executeOperation(Invalidate())

    private fun executeOperation(cacheOperation: Operation) =
            catFactClient().execute(cacheOperation)

}
