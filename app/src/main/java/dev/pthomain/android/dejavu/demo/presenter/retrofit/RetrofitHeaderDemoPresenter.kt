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
import dev.pthomain.android.dejavu.cache.metadata.response.Empty
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.response.Result
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.FreshnessPriority
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.NetworkPriority.LOCAL_ONLY
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Local.Invalidate
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.demo.DemoActivity
import dev.pthomain.android.dejavu.demo.dejavu.clients.model.CatFactResponse
import dev.pthomain.android.dejavu.demo.presenter.base.OperationPresenterDelegate
import io.reactivex.Observable
import io.reactivex.Single

internal class RetrofitHeaderDemoPresenter(demoActivity: DemoActivity,
                                           uiLogger: Logger)
    : BaseRetrofitDemoPresenter(demoActivity, uiLogger) {

    private val delegate = OperationPresenterDelegate(operationClient()::execute)

    override fun getDataObservable(
            cachePriority: CachePriority,
            encrypt: Boolean,
            compress: Boolean
    ) =
            delegate.getDataObservable(cachePriority, encrypt, compress)

    override fun getOfflineSingle(freshness: FreshnessPriority) =
            delegate.getOfflineSingle(freshness)

    override fun getClearEntriesResult() =
            delegate.getClearEntriesResult()

    override fun getInvalidateResult() =
            delegate.getInvalidateResult()

}
