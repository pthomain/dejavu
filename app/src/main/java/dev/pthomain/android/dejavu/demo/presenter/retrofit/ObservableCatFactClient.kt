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

import dev.pthomain.android.dejavu.DejaVu.Companion.DejaVuHeader
import dev.pthomain.android.dejavu.configuration.CacheInstruction
import dev.pthomain.android.dejavu.demo.model.CatFactResponse
import dev.pthomain.android.dejavu.demo.presenter.BaseDemoPresenter.Companion.ENDPOINT
import dev.pthomain.android.dejavu.retrofit.annotations.*
import dev.pthomain.android.dejavu.retrofit.annotations.OptionalBoolean.TRUE
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header

internal interface ObservableCatFactClient {

    // GET

    @GET(ENDPOINT)
    @Cache
    fun get(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(compress = TRUE)
    fun compressed(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(encrypt = TRUE)
    fun encrypted(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            compress = TRUE,
            encrypt = TRUE
    )
    fun compressedEncrypted(): Observable<CatFactResponse>

    // GET freshOnly

    @GET(ENDPOINT)
    @Cache(freshOnly = true)
    fun freshOnly(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            freshOnly = true,
            compress = TRUE
    )
    fun freshOnlyCompressed(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            freshOnly = true,
            encrypt = TRUE
    )
    fun freshOnlyEncrypted(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            freshOnly = true,
            compress = TRUE,
            encrypt = TRUE
    )
    fun freshOnlyCompressedEncrypted(): Observable<CatFactResponse>

    // REFRESH

    @GET(ENDPOINT)
    @Refresh
    fun refresh(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Refresh(freshOnly = true)
    fun refreshFreshOnly(): Observable<CatFactResponse>

    // CLEAR

    @DELETE(ENDPOINT)
    @Clear(typeToClear = CatFactResponse::class)
    fun clearCache(): Completable

    // INVALIDATE

    @DELETE(ENDPOINT)
    @Invalidate(typeToInvalidate = CatFactResponse::class)
    fun invalidate(): Completable

    // OFFLINE

    @GET(ENDPOINT)
    @Offline
    fun offline(): Single<CatFactResponse>

    @GET(ENDPOINT)
    @Offline(freshOnly = true)
    fun offlineFreshOnly(): Single<CatFactResponse>

    //HEADER

    @GET(ENDPOINT)
    fun instruct(@Header(DejaVuHeader) instruction: CacheInstruction): Observable<CatFactResponse>

}
