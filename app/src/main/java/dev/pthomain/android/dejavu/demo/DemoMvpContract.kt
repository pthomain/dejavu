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

package dev.pthomain.android.dejavu.demo


import dev.pthomain.android.boilerplate.core.mvp.base.MvpContract.*
import dev.pthomain.android.boilerplate.core.utils.lambda.Callback1
import dev.pthomain.android.boilerplate.core.utils.log.CompositeLogger
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.demo.model.CatFactResponse
import dev.pthomain.android.dejavu.demo.presenter.CompositePresenter
import dev.pthomain.android.dejavu.demo.presenter.CompositePresenter.Method
import dev.pthomain.android.dejavu.shared.token.instruction.operation.CachePriority.FreshnessPriority
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation
import org.koin.core.Koin

internal class DemoMvpContract {

    interface DemoMvpView : MvpView<DemoMvpView, DemoPresenter, DemoViewComponent> {

        fun showCatFact(response: CatFactResponse)
        fun showResult(result: DejaVuResult<CatFactResponse>)
        fun onCallStarted()
        fun onCallComplete()

    }

    interface DemoPresenter : Presenter<DemoMvpView, DemoPresenter, DemoViewComponent> {

        var useSingle: Boolean
        var useHeader: Boolean
        var encrypt: Boolean
        var compress: Boolean
        var freshness: FreshnessPriority
        var connectivityTimeoutOn: Boolean

        fun loadCatFact(isRefresh: Boolean)
        fun clearEntries()
        fun invalidate()
        fun offline()
        fun getCacheOperation(): Operation

    }

    class DemoViewComponent(
            private val koin: Koin
    ) : ViewComponent<DemoMvpView, DemoPresenter, DemoViewComponent> {

        fun presenterSwitcher() = koin.get<Callback1<Method>>()

        fun logger(): Logger = koin.get<CompositeLogger>()

        override fun presenter() = koin.get<CompositePresenter>()

    }

}