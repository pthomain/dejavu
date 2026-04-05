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

package dev.pthomain.android.dejavu.demo.dejavu.clients.factories

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import com.google.gson.Gson
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.demo.dejavu.clients.retrofit.AnnotationRetrofitClient
import dev.pthomain.android.dejavu.demo.dejavu.clients.retrofit.HeaderRetrofitClient
import dev.pthomain.android.dejavu.demo.presenter.DemoPresenter
import dev.pthomain.android.dejavu.demo.presenter.DemoPresenter.PersistenceType
import dev.pthomain.android.dejavu.persistence.memory.di.MemoryPersistence
import dev.pthomain.android.dejavu.persistence.sqlite.di.SqlitePersistence
import dev.pthomain.android.dejavu.retrofit.DejaVuRetrofit
import dev.pthomain.android.dejavu.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.serialisation.encryption.Encryption
import dev.pthomain.android.dejavu.serialisation.gson.GsonSerialiser
import dev.pthomain.android.glitchy.core.interceptor.error.glitch.Glitch
import dev.pthomain.android.mumbo.Mumbo
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Factory responsible for creating DejaVu-enabled Retrofit clients.
 * Supports switching between persistence backends (SQLite / Memory)
 * and toggling encryption on or off.
 */
class DejaVuFactory(
        private val logger: Logger,
        private val context: Context
) {

    private val gson = Gson()
    private val serialiser = GsonSerialiser(gson)

    private val encryptionDecorator: SerialisationDecorator = Mumbo.builder()
            .withContext(context)
            .withLogger(logger)
            .build()
            .run { Encryption(if (SDK_INT >= 23) tink() else conceal()) }
            .serialisationDecorator

    private fun decorators(encrypt: Boolean): List<SerialisationDecorator> =
            if (encrypt) listOf(encryptionDecorator)
            else emptyList()

    private fun persistenceModule(
            persistence: PersistenceType,
            encrypt: Boolean
    ) = when (persistence) {
        PersistenceType.MEMORY -> MemoryPersistence(decorators(encrypt), serialiser)
        PersistenceType.SQLITE -> SqlitePersistence(decorators(encrypt), serialiser)
    }

    fun createRetrofitClients(
            persistence: PersistenceType,
            encrypt: Boolean
    ): RetrofitClients {
        val dejaVuRetrofit = DejaVuRetrofit.builder<Glitch>(
                context,
                dev.pthomain.android.dejavu.configuration.error.DejaVuGlitchFactory(
                        dev.pthomain.android.glitchy.core.interceptor.error.glitch.GlitchFactory()
                ),
                persistenceModule(persistence, encrypt),
                logger
        ).build()

        val retrofit = Retrofit.Builder()
                .baseUrl(DemoPresenter.BASE_URL)
                .client(createOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(dejaVuRetrofit.callAdapterFactory)
                .build()

        return RetrofitClients(
                annotationClient = retrofit.create(AnnotationRetrofitClient::class.java),
                headerClient = retrofit.create(HeaderRetrofitClient::class.java)
        )
    }

    private fun createOkHttpClient() = OkHttpClient.Builder()
            .addInterceptor(
                    HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
                        override fun log(message: String) {
                            logger.d(this, message)
                        }
                    }).apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
            )
            .followRedirects(true)
            .build()

    data class RetrofitClients(
            val annotationClient: AnnotationRetrofitClient,
            val headerClient: HeaderRetrofitClient
    )
}
