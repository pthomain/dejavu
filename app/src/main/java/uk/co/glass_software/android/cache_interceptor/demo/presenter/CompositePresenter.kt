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

package uk.co.glass_software.android.cache_interceptor.demo.presenter

import io.reactivex.disposables.CompositeDisposable
import uk.co.glass_software.android.boilerplate.utils.lambda.Callback1
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity
import uk.co.glass_software.android.cache_interceptor.demo.DemoMvpContract.DemoPresenter
import uk.co.glass_software.android.cache_interceptor.demo.presenter.CompositePresenter.Method
import uk.co.glass_software.android.cache_interceptor.demo.presenter.CompositePresenter.Method.*
import uk.co.glass_software.android.cache_interceptor.demo.presenter.retrofit.RetrofitAnnotationDemoPresenter
import uk.co.glass_software.android.cache_interceptor.demo.presenter.retrofit.RetrofitHeaderDemoPresenter
import uk.co.glass_software.android.cache_interceptor.demo.presenter.volley.VolleyDemoPresenter

internal class CompositePresenter(override val mvpView: DemoActivity,
                                  private var presenter: DemoPresenter,
                                  private val retrofitAnnotationDemoPresenter: RetrofitAnnotationDemoPresenter,
                                  private val retrofitHeaderDemoPresenter: RetrofitHeaderDemoPresenter,
                                  private val volleyDemoPresenter: VolleyDemoPresenter)
    : DemoPresenter by presenter, Callback1<Method> {

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

}