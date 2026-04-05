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


import dev.pthomain.android.dejavu.utils.Logger
import dev.pthomain.android.dejavu.utils.CompositeLogger
import dev.pthomain.android.dejavu.utils.SimpleLogger
import dev.pthomain.android.dejavu.utils.Printer
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.FreshnessPriority
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.DejaVuFactory.PersistenceType
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.SerialiserType
import dev.pthomain.android.dejavu.demo.dejavu.clients.model.CatFactResponse
import dev.pthomain.android.dejavu.demo.presenter.base.CompositePresenter
import dev.pthomain.android.dejavu.demo.presenter.base.CompositePresenter.Method
import dev.pthomain.android.dejavu.demo.presenter.retrofit.RetrofitAnnotationDemoPresenter
import dev.pthomain.android.dejavu.demo.presenter.retrofit.RetrofitHeaderDemoPresenter
import dev.pthomain.android.dejavu.demo.presenter.volley.VolleyPresenter
import dev.pthomain.android.boilerplate.ui.mvp.base.MvpContract.*

internal class DemoMvpContract {

    interface DemoMvpView : MvpView<DemoMvpView, DemoPresenter, DemoViewComponent> {

        fun showCatFact(response: CatFactResponse)
        fun showResult(result: DejaVuResult<CatFactResponse>)
        fun onCallStarted()
        fun onCallComplete()

    }

    interface DemoPresenter : Presenter<DemoMvpView, DemoPresenter, DemoViewComponent> {

        var useSingle: Boolean
        var serialiserType: SerialiserType
        var method: Method
        var persistence: PersistenceType
        var encrypt: Boolean
        var compress: Boolean
        var freshness: FreshnessPriority

        fun loadCatFact(isRefresh: Boolean)
        fun clearEntries()
        fun invalidate()
        fun offline()
        fun getCacheOperation(): Operation
    }

    class DemoViewComponent(
            private val compositePresenter: CompositePresenter,
            private val presenterSwitcherFn: (Method) -> Unit,
            private val compositeLogger: Logger
    ) : ViewComponent<DemoMvpView, DemoPresenter, DemoViewComponent> {

        fun presenterSwitcher() = presenterSwitcherFn

        fun logger(): Logger = compositeLogger

        override fun presenter() = compositePresenter

        companion object {
            fun create(
                    demoActivity: DemoActivity,
                    onLogOutput: (String) -> Unit
            ): DemoViewComponent {
                val boilerplateLogger = SimpleLogger(
                        true,
                        demoActivity.packageName
                )

                val uiLogger = CompositeLogger(
                        boilerplateLogger,
                        SimpleLogger(
                                true,
                                demoActivity.packageName,
                                object : Printer {
                                    override fun canPrint(className: String) =
                                            !className.contains(SimpleLogger::class.java.`package`!!.name)

                                    override fun print(
                                            priority: Int,
                                            tag: String?,
                                            targetClassName: String,
                                            message: String
                                    ) {
                                        clean(message).also {
                                            if (!it.isBlank()) onLogOutput(it)
                                        }
                                    }

                                    private fun clean(message: String) =
                                            message.replace(Regex("(\\([^)]+\\))"), "")
                                                    .replace(Regex("\\n+"), "\n")
                                                    .trim()
                                }
                        )
                )

                val compositeLogger = CompositeLogger(
                        boilerplateLogger,
                        uiLogger
                )

                val retrofitAnnotationPresenter = RetrofitAnnotationDemoPresenter(
                        demoActivity,
                        uiLogger
                )

                val retrofitHeaderPresenter = RetrofitHeaderDemoPresenter(
                        demoActivity,
                        uiLogger
                )

                val volleyPresenter = VolleyPresenter(
                        demoActivity,
                        uiLogger
                )

                val compositePresenter = CompositePresenter(
                        demoActivity,
                        retrofitAnnotationPresenter,
                        retrofitHeaderPresenter,
                        volleyPresenter
                )

                return DemoViewComponent(
                        compositePresenter,
                        compositePresenter,
                        compositeLogger
                )
            }
        }
    }

}
