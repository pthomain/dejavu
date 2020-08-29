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
import dev.pthomain.android.boilerplate.core.builder.ExtensionBuilder
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.DejaVu
import dev.pthomain.android.dejavu.demo.dejavu.DejaVuRetrofitClient
import dev.pthomain.android.dejavu.demo.dejavu.DejaVuVolleyClient
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.DejaVuFactory.PersistenceType.*
import dev.pthomain.android.dejavu.persistence.file.di.FilePersistence
import dev.pthomain.android.dejavu.persistence.memory.di.MemoryPersistence
import dev.pthomain.android.dejavu.persistence.sqlite.di.SqlitePersistence
import dev.pthomain.android.dejavu.retrofit.DejaVuRetrofit
import dev.pthomain.android.dejavu.serialisation.Serialiser
import dev.pthomain.android.dejavu.serialisation.compression.Compression
import dev.pthomain.android.dejavu.serialisation.encryption.Encryption
import dev.pthomain.android.dejavu.volley.DejaVuVolley
import dev.pthomain.android.glitchy.core.interceptor.interceptors.error.NetworkErrorPredicate
import dev.pthomain.android.mumbo.Mumbo
import org.koin.core.module.Module

class DejaVuFactory(
        private val logger: Logger,
        private val context: Context
) {

    private val compressionDecorator = Compression(logger).serialisationDecorator

    private val encryptionDecorator = Mumbo.builder()
            .withContext(context)
            .withLogger(logger)
            .build()
            .run { Encryption(if (SDK_INT >= 23) tink() else conceal()) }
            .serialisationDecorator

    var encrypt = false
    var compress = false

    private val decorators = listOf(compressionDecorator, encryptionDecorator)

    private fun persistenceModuleProvider(
            persistence: PersistenceType,
            serialiser: Serialiser,
    ) = when (persistence) {
        FILE -> FilePersistence(decorators, serialiser)
        MEMORY -> MemoryPersistence(decorators, serialiser)
        SQLITE -> SqlitePersistence(decorators, serialiser)
    }

    enum class PersistenceType {
        FILE,
        MEMORY,
        SQLITE
    }

    class DejaVuDependencies<E>(
            val persistence: PersistenceType,
            val serialiserType: SerialiserType,
            val errorFactoryType: ErrorFactoryType<E>,
    ) where E : Throwable,
            E : NetworkErrorPredicate

    private fun <E> dejaVuBuilder(dependencies: DejaVuDependencies<E>)
            where E : Throwable,
                  E : NetworkErrorPredicate = with(dependencies) {
        DejaVu.builder(
                context,
                errorFactoryType.errorFactory,
                persistenceModuleProvider(
                        persistence,
                        serialiserType.serialiser
                ),
                logger
        )
    }

    fun <E, C, B : ExtensionBuilder<D, Module, B>, D> createClient(
            dependencies: DejaVuDependencies<E>,
            extensionBuilder: B,
            clientBuilder: (D, Logger) -> C,
    ) where E : Throwable,
            E : NetworkErrorPredicate =
            clientBuilder(
                    dejaVuBuilder(dependencies)
                            .extend(extensionBuilder)
                            .build(),
                    logger
            )

    fun <E> createRetrofit(dependencies: DejaVuDependencies<E>)
            where E : Throwable,
                  E : NetworkErrorPredicate =
            createClient(
                    dependencies,
                    DejaVuRetrofit.extension<E>(),
                    ::DejaVuRetrofitClient
            )

    fun <E> createVolley(dependencies: DejaVuDependencies<E>)
            where E : Throwable,
                  E : NetworkErrorPredicate =
            createClient(
                    dependencies,
                    DejaVuVolley.extension<E>(),
                    ::DejaVuVolleyClient
            )
}