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

import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.Behaviour.INVALIDATE
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.FreshnessPriority
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.FreshnessPriority.FRESH_ONLY
import dev.pthomain.android.dejavu.demo.DemoActivity

internal class RetrofitAnnotationDemoPresenter(
        demoActivity: DemoActivity,
        uiLogger: Logger
) : BaseRetrofitDemoPresenter(demoActivity, uiLogger) {

    override fun getDataObservable(cachePriority: CachePriority,
                                   encrypt: Boolean,
                                   compress: Boolean) =
            with(cachePriority) {
                val client = dataClient()
                if (behaviour == INVALIDATE) {
                    when {
                        freshness.isFreshOnly() && compress && encrypt -> client.refreshCompressedEncryptedFreshOnly()
                        freshness.isFreshOnly() && compress -> client.refreshCompressedFreshOnly()
                        freshness.isFreshOnly() && encrypt -> client.refreshEncryptedFreshOnly()
                        freshness.isFreshOnly() -> client.refreshFreshOnly()
                        compress && encrypt -> client.refreshCompressedEncrypted()
                        compress -> client.refreshCompressed()
                        encrypt -> client.refreshEncrypted()
                        else -> client.refresh()
                    }
                } else {
                    when {
                        freshness.isFreshOnly() && compress && encrypt -> client.freshOnlyCompressedEncrypted()
                        freshness.isFreshOnly() && compress -> client.freshOnlyCompressed()
                        freshness.isFreshOnly() && encrypt -> client.freshOnlyEncrypted()
                        freshness.isFreshOnly() -> client.freshOnly()
                        compress && encrypt -> client.compressedEncrypted()
                        compress -> client.compressed()
                        encrypt -> client.encrypted()
                        else -> client.get()
                    }
                }
            }

    override fun getOfflineSingle(freshness: FreshnessPriority) = ifElse(
            freshness == FRESH_ONLY,
            dataClient().offlineFreshOnly(),
            dataClient().offline()
    )

    override fun getClearEntriesResult() =
            operationClient().clearCache()

    override fun getInvalidateResult() =
            operationClient().invalidate()
}
