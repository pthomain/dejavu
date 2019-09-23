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

internal class RetrofitAnnotationDemoPresenter(demoActivity: DemoActivity,
                                               uiLogger: Logger)
    : BaseRetrofitDemoPresenter(demoActivity, uiLogger) {

    override fun getResponseObservable(isRefresh: Boolean,
                                       encrypt: Boolean,
                                       compress: Boolean,
                                       freshOnly: Boolean) =
            if (isRefresh) {
                when {
                    freshOnly -> catFactClient().refreshFreshOnly()
                    else -> catFactClient().refresh()
                }
            } else {
                when {
                    freshOnly && compress && encrypt -> catFactClient().freshOnlyCompressedEncrypted()
                    freshOnly && compress -> catFactClient().freshOnlyCompressed()
                    freshOnly && encrypt -> catFactClient().freshOnlyEncrypted()
                    freshOnly -> catFactClient().freshOnly()
                    compress && encrypt -> catFactClient().compressedEncrypted()
                    compress -> catFactClient().compressed()
                    encrypt -> catFactClient().encrypted()
                    else -> catFactClient().get()
                }
            }

    override fun getOfflineSingle(freshOnly: Boolean) =
            if (freshOnly) catFactClient().offlineFreshOnly()
            else catFactClient().offline()

    override fun getClearEntriesCompletable() = catFactClient().clearCache()

    override fun getInvalidateCompletable() = catFactClient().invalidate()

}
