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

package dev.pthomain.android.dejavu.demo.presenter.retrofit.clients

import dev.pthomain.android.dejavu.DejaVu.Companion.DejaVuHeader
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.demo.model.CatFactResponse
import dev.pthomain.android.dejavu.demo.presenter.BaseDemoPresenter.Companion.ENDPOINT
import dev.pthomain.android.dejavu.retrofit.annotations.Cache
import dev.pthomain.android.dejavu.retrofit.annotations.Clear
import dev.pthomain.android.dejavu.retrofit.annotations.Invalidate
import dev.pthomain.android.dejavu.shared.token.instruction.operation.CachePriority.*
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation
import io.reactivex.Single
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header

interface SingleClients {

    interface Data {
        // GET

        @GET(ENDPOINT)
        @Cache
        fun get(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(compress = true)
        fun compressed(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(encrypt = true)
        fun encrypted(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                compress = true,
                encrypt = true
        )
        fun compressedEncrypted(): Single<CatFactResponse>

        // GET freshOnly

        @GET(ENDPOINT)
        @Cache(priority = STALE_NOT_ACCEPTED)
        fun freshOnly(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = STALE_NOT_ACCEPTED,
                compress = true
        )
        fun freshOnlyCompressed(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = STALE_NOT_ACCEPTED,
                encrypt = true
        )
        fun freshOnlyEncrypted(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = STALE_NOT_ACCEPTED,
                compress = true,
                encrypt = true
        )
        fun freshOnlyCompressedEncrypted(): Single<CatFactResponse>

        // REFRESH

        @GET(ENDPOINT)
        @Cache(priority = INVALIDATE_STALE_ACCEPTED_FIRST)
        fun refresh(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(priority = INVALIDATE_STALE_NOT_ACCEPTED)
        fun refreshFreshOnly(): Single<CatFactResponse>

        // OFFLINE

        @GET(ENDPOINT)
        @Cache(priority = OFFLINE_STALE_ACCEPTED)
        fun offline(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(priority = OFFLINE_STALE_NOT_ACCEPTED)
        fun offlineFreshOnly(): Single<CatFactResponse>
    }

    interface Operations {
        // CLEAR

        @DELETE(ENDPOINT)
        @Clear
        fun clearCache(): Single<DejaVuResult<CatFactResponse>>

        // INVALIDATE

        @DELETE(ENDPOINT)
        @Invalidate
        fun invalidate(): Single<DejaVuResult<CatFactResponse>>

        //HEADER

        @GET(ENDPOINT)
        fun execute(@Header(DejaVuHeader) operation: Operation): Single<DejaVuResult<CatFactResponse>>
    }
}
