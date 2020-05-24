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

package dev.pthomain.android.dejavu.demo.dejavu.clients.retrofit

import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.*
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.demo.dejavu.clients.base.SingleClients
import dev.pthomain.android.dejavu.demo.dejavu.clients.model.CatFactResponse
import dev.pthomain.android.dejavu.demo.presenter.base.BaseDemoPresenter.Companion.ENDPOINT
import dev.pthomain.android.dejavu.retrofit.annotations.Cache
import dev.pthomain.android.dejavu.retrofit.annotations.Clear
import dev.pthomain.android.dejavu.retrofit.annotations.Invalidate
import dev.pthomain.android.dejavu.retrofit.operation.DejaVuHeader
import io.reactivex.Single
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header

interface RetrofitSingleClients : SingleClients {

    interface Data : SingleClients.Data {
        // GET

        @GET(ENDPOINT)
        @Cache
        override fun get(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(serialisation = "compress")
        override fun compressed(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(serialisation = "encrypt")
        override fun encrypted(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(serialisation = "compress,encrypt")
        override fun compressedEncrypted(): Single<CatFactResponse>

        // GET freshOnly

        @GET(ENDPOINT)
        @Cache(priority = STALE_NOT_ACCEPTED)
        override fun freshOnly(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = STALE_NOT_ACCEPTED,
                serialisation = "compress"
        )
        override fun freshOnlyCompressed(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = STALE_NOT_ACCEPTED,
                serialisation = "encrypt"
        )
        override fun freshOnlyEncrypted(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = STALE_NOT_ACCEPTED,
                serialisation = "compress,encrypt"
        )
        override fun freshOnlyCompressedEncrypted(): Single<CatFactResponse>

        // REFRESH

        @GET(ENDPOINT)
        @Cache(priority = INVALIDATE_STALE_ACCEPTED_FIRST)
        override fun refresh(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(priority = INVALIDATE_STALE_NOT_ACCEPTED)
        override fun refreshFreshOnly(): Single<CatFactResponse>

        // OFFLINE

        @GET(ENDPOINT)
        @Cache(priority = OFFLINE_STALE_ACCEPTED)
        override fun offline(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(priority = OFFLINE_STALE_NOT_ACCEPTED)
        override fun offlineFreshOnly(): Single<CatFactResponse>
    }

    interface Operations : SingleClients.Operations {
        // CLEAR

        @DELETE(ENDPOINT)
        @Clear
        override fun clearCache(): Single<DejaVuResult<CatFactResponse>>

        // INVALIDATE

        @DELETE(ENDPOINT)
        @Invalidate
        override fun invalidate(): Single<DejaVuResult<CatFactResponse>>

        //HEADER

        @GET(ENDPOINT)
        fun execute(@Header(DejaVuHeader) operation: Operation): Single<DejaVuResult<CatFactResponse>>
    }
}
