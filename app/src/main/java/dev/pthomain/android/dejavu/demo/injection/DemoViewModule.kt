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

package dev.pthomain.android.dejavu.demo.injection

import dev.pthomain.android.boilerplate.core.utils.log.CompositeLogger
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.boilerplate.core.utils.log.Printer
import dev.pthomain.android.boilerplate.core.utils.log.SimpleLogger
import dev.pthomain.android.dejavu.demo.DemoActivity
import dev.pthomain.android.dejavu.demo.presenter.base.CompositePresenter
import dev.pthomain.android.dejavu.demo.presenter.base.CompositePresenter.Method
import dev.pthomain.android.dejavu.demo.presenter.retrofit.RetrofitAnnotationDemoPresenter
import dev.pthomain.android.dejavu.demo.presenter.retrofit.RetrofitHeaderDemoPresenter
import dev.pthomain.android.dejavu.demo.presenter.volley.VolleyPresenter
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal class DemoViewModule(
        private val demoActivity: DemoActivity,
        private val onLogOutput: (String) -> Unit
) {

    val module = module {

        single<Logger>(named("boilerplate")) {
            SimpleLogger(
                    true,
                    demoActivity.packageName
            )
        }

        single<Logger>(named("ui")) {
            CompositeLogger(
                    get(named("boilerplate")),
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
        }

        single {
            CompositeLogger(
                    get(named("boilerplate")),
                    get(named("ui"))
            )
        }

        single {
            RetrofitAnnotationDemoPresenter(
                    demoActivity,
                    get(named("ui"))
            )
        }

        single {
            RetrofitHeaderDemoPresenter(
                    demoActivity,
                    get(named("ui"))
            )
        }

        single {
            VolleyPresenter(
                    demoActivity,
                    get(named("ui"))
            )
        }

        single {
            CompositePresenter(
                    demoActivity,
                    get(),
                    get(),
                    get()
            )
        }

        single<(Method) -> Unit> {
            get<CompositePresenter>()
        }
    }
}