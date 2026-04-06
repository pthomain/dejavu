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
import dev.pthomain.android.dejavu.demo.dejavu.clients.ENDPOINT
import dev.pthomain.android.dejavu.demo.dejavu.clients.model.CatFactResponse
import dev.pthomain.android.dejavu.retrofit.annotations.Cache
import dev.pthomain.android.dejavu.retrofit.annotations.Clear
import dev.pthomain.android.dejavu.retrofit.annotations.Invalidate
import dev.pthomain.android.dejavu.retrofit.operation.DejaVuHeader
import kotlinx.coroutines.flow.Flow
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * Retrofit client using DejaVu annotations for cache configuration.
 */
interface AnnotationRetrofitClient {

    // Basic cache

    @GET(ENDPOINT)
    @Cache
    fun get(): Flow<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(serialisation = "encrypt")
    fun getEncrypted(): Flow<CatFactResponse>

    // Fresh only

    @GET(ENDPOINT)
    @Cache(priority = STALE_NOT_ACCEPTED)
    fun getFreshOnly(): Flow<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(priority = STALE_NOT_ACCEPTED, serialisation = "encrypt")
    fun getFreshOnlyEncrypted(): Flow<CatFactResponse>

    // Refresh (invalidate + fetch)

    @GET(ENDPOINT)
    @Cache(priority = INVALIDATE_STALE_ACCEPTED_FIRST)
    fun refresh(): Flow<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(priority = INVALIDATE_STALE_ACCEPTED_FIRST, serialisation = "encrypt")
    fun refreshEncrypted(): Flow<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(priority = INVALIDATE_STALE_NOT_ACCEPTED)
    fun refreshFreshOnly(): Flow<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(priority = INVALIDATE_STALE_NOT_ACCEPTED, serialisation = "encrypt")
    fun refreshFreshOnlyEncrypted(): Flow<CatFactResponse>

    // Offline

    @GET(ENDPOINT)
    @Cache(priority = OFFLINE_STALE_ACCEPTED)
    fun offline(): Flow<CatFactResponse>

    @GET(ENDPOINT)
    @Cache(priority = OFFLINE_STALE_NOT_ACCEPTED)
    fun offlineFreshOnly(): Flow<CatFactResponse>

    // Operations

    @DELETE(ENDPOINT)
    @Clear
    fun clearCache(): Flow<DejaVuResult<CatFactResponse>>

    @DELETE(ENDPOINT)
    @Invalidate
    fun invalidate(): Flow<DejaVuResult<CatFactResponse>>
}

/**
 * Retrofit client using runtime headers for cache configuration.
 */
interface HeaderRetrofitClient {

    @GET(ENDPOINT)
    fun execute(@Header(DejaVuHeader) operation: Operation): Flow<DejaVuResult<CatFactResponse>>
}
