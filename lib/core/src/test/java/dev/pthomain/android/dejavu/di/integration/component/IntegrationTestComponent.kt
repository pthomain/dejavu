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

package dev.pthomain.android.dejavu.di.integration.component

import dagger.Component
import dev.pthomain.android.dejavu.di.integration.module.IntegrationTestModule
import dev.pthomain.android.dejavu.test.AssetHelper
import dev.pthomain.android.dejavu.test.network.MockClient
import dev.pthomain.android.dejavu.test.network.retrofit.TestClient
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Singleton
@Component(modules = [IntegrationTestModule::class])
internal interface IntegrationTestComponent {
    fun okHttpClient(): OkHttpClient
    fun retrofit(): Retrofit
    fun mockClient(): MockClient
    fun testClient(): TestClient
    fun assetHelper(): AssetHelper
}
