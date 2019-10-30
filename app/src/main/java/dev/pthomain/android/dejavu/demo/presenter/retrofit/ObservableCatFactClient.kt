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
import dev.pthomain.android.dejavu.configuration.instruction.CacheOperation
import dev.pthomain.android.dejavu.configuration.instruction.CachePriority.*
import dev.pthomain.android.dejavu.configuration.instruction.Operation
import dev.pthomain.android.dejavu.demo.model.CatFactResponse
import dev.pthomain.android.dejavu.demo.presenter.BaseDemoPresenter.Companion.ENDPOINT
import dev.pthomain.android.dejavu.retrofit.annotations.Cache
import dev.pthomain.android.dejavu.retrofit.annotations.Clear
import dev.pthomain.android.dejavu.retrofit.annotations.Invalidate
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
    @Cache(compress = true)
    fun compressed(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(encrypt = true)
    fun encrypted(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            compress = true,
            encrypt = true
    )
    fun compressedEncrypted(): Observable<CatFactResponse>

    // GET freshOnly

    @GET(ENDPOINT)
    @Cache(priority = FRESH_ONLY)
    fun freshOnly(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            priority = FRESH_ONLY,
            compress = true
    )
    fun freshOnlyCompressed(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            priority = FRESH_ONLY,
            encrypt = true
    )
    fun freshOnlyEncrypted(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            priority = FRESH_ONLY,
            compress = true,
            encrypt = true
    )
    fun freshOnlyCompressedEncrypted(): Observable<CatFactResponse>

    // REFRESH

    @GET(ENDPOINT)
    @Cache(priority = REFRESH_FRESH_PREFERRED)
    fun refresh(): Observable<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(priority = REFRESH_FRESH_ONLY)
    fun refreshFreshOnly(): Observable<CatFactResponse>

    // CLEAR

    @DELETE(ENDPOINT)
    @Clear
    fun clearCache(): CacheOperation<CatFactResponse>

    // INVALIDATE

    @DELETE(ENDPOINT)
    @Invalidate
    fun invalidate(): CacheOperation<CatFactResponse>

    // OFFLINE

    @GET(ENDPOINT)
    @Cache(priority = OFFLINE)
    fun offline(): Single<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(priority = OFFLINE_FRESH_ONLY)
    fun offlineFreshOnly(): Single<CatFactResponse>

    //HEADER

    @GET(ENDPOINT)
    fun execute(@Header(DejaVuHeader) operation: Operation): CacheOperation<CatFactResponse>

}
