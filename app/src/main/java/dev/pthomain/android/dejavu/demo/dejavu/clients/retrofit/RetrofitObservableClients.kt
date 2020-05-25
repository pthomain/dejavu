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
import dev.pthomain.android.dejavu.demo.dejavu.clients.base.ObservableClients
import dev.pthomain.android.dejavu.demo.dejavu.clients.model.CatFactResponse
import dev.pthomain.android.dejavu.demo.presenter.base.BaseDemoPresenter.Companion.ENDPOINT
import dev.pthomain.android.dejavu.retrofit.annotations.Cache
import dev.pthomain.android.dejavu.retrofit.annotations.Clear
import dev.pthomain.android.dejavu.retrofit.annotations.Invalidate
import dev.pthomain.android.dejavu.retrofit.operation.DejaVuHeader
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header

interface RetrofitObservableClients : ObservableClients {

    interface Data : ObservableClients.Data {
        // GET

        @GET(ENDPOINT)
        @Cache
        override fun get(): Observable<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(serialisation = "compress")
        override fun compressed(): Observable<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(serialisation = "encrypt")
        override fun encrypted(): Observable<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(serialisation = "compress,encrypt")
        override fun compressedEncrypted(): Observable<CatFactResponse>

        // GET freshOnly

        @GET(ENDPOINT)
        @Cache(priority = STALE_NOT_ACCEPTED)
        override fun freshOnly(): Observable<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = STALE_NOT_ACCEPTED,
                serialisation = "compress"
        )
        override fun freshOnlyCompressed(): Observable<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = STALE_NOT_ACCEPTED,
                serialisation = "encrypt"
        )
        override fun freshOnlyEncrypted(): Observable<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = STALE_NOT_ACCEPTED,
                serialisation = "compress,encrypt"
        )
        override fun freshOnlyCompressedEncrypted(): Observable<CatFactResponse>

        // REFRESH

        @GET(ENDPOINT)
        @Cache(priority = INVALIDATE_STALE_ACCEPTED_FIRST)
        override fun refresh(): Observable<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = INVALIDATE_STALE_ACCEPTED_FIRST,
                serialisation = "compress"
        )
        override fun refreshCompressed(): Observable<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = INVALIDATE_STALE_ACCEPTED_FIRST,
                serialisation = "encrypt"
        )
        override fun refreshEncrypted(): Observable<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = INVALIDATE_STALE_ACCEPTED_FIRST,
                serialisation = "compress,encrypt"
        )
        override fun refreshCompressedEncrypted(): Observable<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(priority = INVALIDATE_STALE_NOT_ACCEPTED)
        override fun refreshFreshOnly(): Observable<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = INVALIDATE_STALE_NOT_ACCEPTED,
                serialisation = "compress"
        )
        override fun refreshCompressedFreshOnly(): Observable<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = INVALIDATE_STALE_NOT_ACCEPTED,
                serialisation = "encrypt"
        )
        override fun refreshEncryptedFreshOnly(): Observable<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(
                priority = INVALIDATE_STALE_NOT_ACCEPTED,
                serialisation = "compress,encrypt"
        )
        override fun refreshCompressedEncryptedFreshOnly(): Observable<CatFactResponse>

        // OFFLINE

        @GET(ENDPOINT)
        @Cache(priority = OFFLINE_STALE_ACCEPTED)
        override fun offline(): Single<CatFactResponse>

        @GET(ENDPOINT)
        @Cache(priority = OFFLINE_STALE_NOT_ACCEPTED)
        override fun offlineFreshOnly(): Single<CatFactResponse>
    }

    interface Operations : ObservableClients.Operations {
        // CLEAR

        @DELETE(ENDPOINT)
        @Clear
        override fun clearCache(): Observable<DejaVuResult<CatFactResponse>>

        // INVALIDATE

        @DELETE(ENDPOINT)
        @Invalidate
        override fun invalidate(): Observable<DejaVuResult<CatFactResponse>>

        //HEADER

        @GET(ENDPOINT)
        fun execute(@Header(DejaVuHeader) operation: Operation): Observable<DejaVuResult<CatFactResponse>>
    }
}
