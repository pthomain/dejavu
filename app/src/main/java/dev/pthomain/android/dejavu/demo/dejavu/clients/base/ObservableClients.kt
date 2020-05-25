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

package dev.pthomain.android.dejavu.demo.dejavu.clients.base

import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.demo.dejavu.clients.model.CatFactResponse
import io.reactivex.Observable
import io.reactivex.Single

interface ObservableClients {

    interface Data {
        // GET

        fun get(): Observable<CatFactResponse>
        fun compressed(): Observable<CatFactResponse>
        fun encrypted(): Observable<CatFactResponse>
        fun compressedEncrypted(): Observable<CatFactResponse>

        // GET freshOnly

        fun freshOnly(): Observable<CatFactResponse>
        fun freshOnlyCompressed(): Observable<CatFactResponse>
        fun freshOnlyEncrypted(): Observable<CatFactResponse>
        fun freshOnlyCompressedEncrypted(): Observable<CatFactResponse>

        // REFRESH

        fun refresh(): Observable<CatFactResponse>
        fun refreshCompressed(): Observable<CatFactResponse>
        fun refreshEncrypted(): Observable<CatFactResponse>
        fun refreshCompressedEncrypted(): Observable<CatFactResponse>

        fun refreshFreshOnly(): Observable<CatFactResponse>
        fun refreshCompressedFreshOnly(): Observable<CatFactResponse>
        fun refreshEncryptedFreshOnly(): Observable<CatFactResponse>
        fun refreshCompressedEncryptedFreshOnly(): Observable<CatFactResponse>

        // OFFLINE

        fun offline(): Single<CatFactResponse>
        fun offlineFreshOnly(): Single<CatFactResponse>
    }

    interface Operations {
        // CLEAR

        fun clearCache(): Observable<DejaVuResult<CatFactResponse>>

        // INVALIDATE

        fun invalidate(): Observable<DejaVuResult<CatFactResponse>>
    }
}
