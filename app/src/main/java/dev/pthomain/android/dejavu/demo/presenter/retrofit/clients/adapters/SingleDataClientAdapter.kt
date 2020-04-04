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

package dev.pthomain.android.dejavu.demo.presenter.retrofit.clients.adapters

import dev.pthomain.android.dejavu.demo.presenter.retrofit.clients.ObservableClients
import dev.pthomain.android.dejavu.demo.presenter.retrofit.clients.SingleClients

class SingleDataClientAdapter(private val singleClient: SingleClients.Data) : ObservableClients.Data {
    override fun get() =
            singleClient.get().toObservable()

    override fun compressed() =
            singleClient.compressed().toObservable()

    override fun encrypted() =
            singleClient.encrypted().toObservable()

    override fun compressedEncrypted() =
            singleClient.compressedEncrypted().toObservable()

    override fun freshOnly() =
            singleClient.freshOnly().toObservable()

    override fun freshOnlyCompressed() =
            singleClient.freshOnlyCompressed().toObservable()

    override fun freshOnlyEncrypted() =
            singleClient.freshOnlyEncrypted().toObservable()

    override fun freshOnlyCompressedEncrypted() =
            singleClient.freshOnlyCompressedEncrypted().toObservable()

    override fun refresh() =
            singleClient.refresh().toObservable()

    override fun refreshFreshOnly() =
            singleClient.refreshFreshOnly().toObservable()

    override fun offline() =
            singleClient.offline()

    override fun offlineFreshOnly() =
            singleClient.offlineFreshOnly()
}