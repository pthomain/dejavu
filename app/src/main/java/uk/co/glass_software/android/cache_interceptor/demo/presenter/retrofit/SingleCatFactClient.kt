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

package uk.co.glass_software.android.cache_interceptor.demo.presenter.retrofit

import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import uk.co.glass_software.android.cache_interceptor.RxCache.Companion.RxCacheHeader
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse
import uk.co.glass_software.android.cache_interceptor.demo.presenter.BaseDemoPresenter.Companion.ENDPOINT
import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.*
import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.OptionalBoolean.TRUE

internal interface SingleCatFactClient {

    // GET

    @GET(ENDPOINT)
    @Cache
    fun get(): Single<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(compress = TRUE)
    fun compressed(): Single<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(encrypt = TRUE)
    fun encrypted(): Single<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            compress = TRUE,
            encrypt = TRUE
    )
    fun compressedEncrypted(): Single<CatFactResponse>

    // GET freshOnly

    @GET(ENDPOINT)
    @Cache(freshOnly = true)
    fun freshOnly(): Single<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            freshOnly = true,
            compress = TRUE
    )
    fun freshOnlyCompressed(): Single<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            freshOnly = true,
            encrypt = TRUE
    )
    fun freshOnlyEncrypted(): Single<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(
            freshOnly = true,
            compress = TRUE,
            encrypt = TRUE
    )
    fun freshOnlyCompressedEncrypted(): Single<CatFactResponse>

    // REFRESH

    @GET(ENDPOINT)
    @Refresh
    fun refresh(): Single<CatFactResponse>

    @GET(ENDPOINT)
    @Refresh(freshOnly = true)
    fun refreshFreshOnly(): Single<CatFactResponse>

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
    fun instruct(@Header(RxCacheHeader) instruction: CacheInstruction): Single<CatFactResponse>

}
