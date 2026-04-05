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
import kotlinx.coroutines.flow.Flow

interface ObservableClients {

    interface Data {
        // GET

        fun get(): Flow<CatFactResponse>
        fun compressed(): Flow<CatFactResponse>
        fun encrypted(): Flow<CatFactResponse>
        fun compressedEncrypted(): Flow<CatFactResponse>

        // GET freshOnly

        fun freshOnly(): Flow<CatFactResponse>
        fun freshOnlyCompressed(): Flow<CatFactResponse>
        fun freshOnlyEncrypted(): Flow<CatFactResponse>
        fun freshOnlyCompressedEncrypted(): Flow<CatFactResponse>

        // REFRESH

        fun refresh(): Flow<CatFactResponse>
        fun refreshCompressed(): Flow<CatFactResponse>
        fun refreshEncrypted(): Flow<CatFactResponse>
        fun refreshCompressedEncrypted(): Flow<CatFactResponse>

        fun refreshFreshOnly(): Flow<CatFactResponse>
        fun refreshCompressedFreshOnly(): Flow<CatFactResponse>
        fun refreshEncryptedFreshOnly(): Flow<CatFactResponse>
        fun refreshCompressedEncryptedFreshOnly(): Flow<CatFactResponse>

        // OFFLINE

        fun offline(): Flow<CatFactResponse>
        fun offlineFreshOnly(): Flow<CatFactResponse>
    }

    interface Operations {
        // CLEAR

        fun clearCache(): Flow<DejaVuResult<CatFactResponse>>

        // INVALIDATE

        fun invalidate(): Flow<DejaVuResult<CatFactResponse>>
    }
}
