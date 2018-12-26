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

package uk.co.glass_software.android.dejavu.demo.presenter

import io.reactivex.disposables.CompositeDisposable
import uk.co.glass_software.android.boilerplate.utils.lambda.Callback1
import uk.co.glass_software.android.dejavu.demo.DemoActivity
import uk.co.glass_software.android.dejavu.demo.DemoMvpContract.DemoPresenter
import uk.co.glass_software.android.dejavu.demo.presenter.CompositePresenter.Method
import uk.co.glass_software.android.dejavu.demo.presenter.CompositePresenter.Method.*
import uk.co.glass_software.android.dejavu.demo.presenter.retrofit.RetrofitAnnotationDemoPresenter
import uk.co.glass_software.android.dejavu.demo.presenter.retrofit.RetrofitHeaderDemoPresenter
import uk.co.glass_software.android.dejavu.demo.presenter.volley.VolleyDemoPresenter

internal class CompositePresenter(override val mvpView: DemoActivity,
                                  private var presenter: DemoPresenter,
                                  private val retrofitAnnotationDemoPresenter: RetrofitAnnotationDemoPresenter,
                                  private val retrofitHeaderDemoPresenter: RetrofitHeaderDemoPresenter,
                                  private val volleyDemoPresenter: VolleyDemoPresenter)
    : DemoPresenter, Callback1<Method> {

    override var subscriptions = CompositeDisposable()

    enum class Method {
        RETROFIT_ANNOTATION,
        RETROFIT_HEADER,
        VOLLEY
    }

    override fun invoke(p1: Method) {
        presenter = when (p1) {
            RETROFIT_ANNOTATION -> retrofitAnnotationDemoPresenter
            RETROFIT_HEADER -> retrofitHeaderDemoPresenter
            VOLLEY -> volleyDemoPresenter
        }
    }

    override var useSingle = presenter.useSingle
    override var allowNonFinalForSingle = presenter.allowNonFinalForSingle
    override var encrypt = presenter.encrypt
    override var compress = presenter.compress
    override var freshOnly = presenter.freshOnly
    override var connectivityTimeoutOn = presenter.connectivityTimeoutOn

    override fun loadCatFact(isRefresh: Boolean) {
        presenter.loadCatFact(isRefresh)
    }

    override fun clearEntries() {
        presenter.clearEntries()
    }

    override fun invalidate() {
        presenter.invalidate()
    }

    override fun offline() {
        presenter.offline()
    }

    override fun getCacheInstruction() = presenter.getCacheInstruction()
}