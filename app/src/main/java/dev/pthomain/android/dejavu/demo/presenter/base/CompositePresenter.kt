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

package dev.pthomain.android.dejavu.demo.presenter.base

import dev.pthomain.android.dejavu.demo.DemoActivity
import dev.pthomain.android.dejavu.demo.DemoMvpContract.DemoPresenter
import dev.pthomain.android.dejavu.demo.presenter.base.CompositePresenter.Method
import dev.pthomain.android.dejavu.demo.presenter.base.CompositePresenter.Method.*
import dev.pthomain.android.dejavu.demo.presenter.retrofit.RetrofitAnnotationDemoPresenter
import dev.pthomain.android.dejavu.demo.presenter.retrofit.RetrofitHeaderDemoPresenter
import dev.pthomain.android.dejavu.demo.presenter.volley.VolleyPresenter
import io.reactivex.disposables.CompositeDisposable

internal class CompositePresenter(
        override val mvpView: DemoActivity,
        private val retrofitAnnotationDemoPresenter: RetrofitAnnotationDemoPresenter,
        private val retrofitHeaderDemoPresenter: RetrofitHeaderDemoPresenter,
        private val volleyPresenter: VolleyPresenter
) : DemoPresenter, (Method) -> Unit {

    private var presenter: DemoPresenter = retrofitAnnotationDemoPresenter

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
            VOLLEY -> volleyPresenter
        }.apply {
            method = p1
        }
        method = presenter.method
    }

    override var useSingle = presenter.useSingle
        set(value) {
            field = value
            presenter.useSingle = value
        }

    override var method = presenter.method
        set(value) {
            field = value
            presenter.method = value
        }

    override var encrypt = presenter.encrypt
        set(value) {
            field = value
            presenter.encrypt = value
        }

    override var compress = presenter.compress
        set(value) {
            field = value
            presenter.compress = value
        }

    override var freshness = presenter.freshness
        set(value) {
            field = value
            presenter.freshness = value
        }

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

    override fun getCacheOperation() = presenter.getCacheOperation()
}
