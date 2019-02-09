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

package uk.co.glass_software.android.dejavu.demo.injection

import dagger.Module
import dagger.Provides
import uk.co.glass_software.android.boilerplate.Boilerplate
import uk.co.glass_software.android.boilerplate.utils.lambda.Callback1
import uk.co.glass_software.android.boilerplate.utils.log.CompositeLogger
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.boilerplate.utils.log.Printer
import uk.co.glass_software.android.boilerplate.utils.log.SimpleLogger
import uk.co.glass_software.android.dejavu.demo.DemoActivity
import uk.co.glass_software.android.dejavu.demo.DemoMvpContract
import uk.co.glass_software.android.dejavu.demo.presenter.CompositePresenter
import uk.co.glass_software.android.dejavu.demo.presenter.CompositePresenter.Method
import uk.co.glass_software.android.dejavu.demo.presenter.retrofit.RetrofitAnnotationDemoPresenter
import uk.co.glass_software.android.dejavu.demo.presenter.retrofit.RetrofitHeaderDemoPresenter
import uk.co.glass_software.android.dejavu.demo.presenter.volley.VolleyDemoPresenter
import javax.inject.Named
import javax.inject.Singleton

@Module
internal class DemoViewModule constructor(private val demoActivity: DemoActivity,
                                          private val onLogOutput: (String) -> Unit) {

    @Provides
    @Singleton
    @Named("boilerplate")
    fun providerBoilerplateLogger() =
            Boilerplate.init(
                    demoActivity,
                    true,
                    "DejaVuLog"
            ).let { Boilerplate.logger }

    @Provides
    @Singleton
    @Named("ui")
    fun providerUiLogger() =
            SimpleLogger(
                    true,
                    object : Printer {
                        override fun canPrint(className: String) = true

                        override fun print(priority: Int,
                                           tag: String?,
                                           message: String) {
                            clean(message).also {
                                if (!it.isNullOrBlank())
                                    onLogOutput(it)
                            }
                        }

                        private fun clean(message: String) =
                                message.replace(Regex("(\\([^)]+\\))"), "")
                                        .replace(Regex("\\n+"), "\n")
                                        .trim()
                    }
            ) as Logger

    @Provides
    @Singleton
    fun providerCompositeLogger(@Named("boilerplate") boilerplateLogger: Logger,
                                @Named("ui") uiLogger: Logger) =
            CompositeLogger(
                    boilerplateLogger,
                    uiLogger
            )

    @Provides
    @Singleton
    fun provideRetrofitAnnotationPresenter(compositeLogger: CompositeLogger) =
            RetrofitAnnotationDemoPresenter(
                    demoActivity,
                    compositeLogger
            )

    @Provides
    @Singleton
    fun provideRetrofitHeaderPresenter(compositeLogger: CompositeLogger) =
            RetrofitHeaderDemoPresenter(
                    demoActivity,
                    compositeLogger
            )

    @Provides
    @Singleton
    fun provideVolleyPresenter(compositeLogger: CompositeLogger) =
            VolleyDemoPresenter(
                    demoActivity,
                    compositeLogger
            )

    @Provides
    @Singleton
    fun provideCompositePresenter(retrofitAnnotationDemoPresenter: RetrofitAnnotationDemoPresenter,
                                  retrofitHeaderDemoPresenter: RetrofitHeaderDemoPresenter,
                                  volleyDemoPresenter: VolleyDemoPresenter) =
            CompositePresenter(
                    demoActivity,
                    retrofitAnnotationDemoPresenter,
                    retrofitAnnotationDemoPresenter,
                    retrofitHeaderDemoPresenter,
                    volleyDemoPresenter
            )

    @Provides
    @Singleton
    fun provideDemoPresenter(compositePresenter: CompositePresenter) =
            compositePresenter as DemoMvpContract.DemoPresenter

    @Provides
    @Singleton
    fun providePresenterSwitcher(compositePresenter: CompositePresenter) =
            compositePresenter as Callback1<Method>

}