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
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.mumbo.Mumbo

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
            serialiser: Serialiser
    ) =
            when (persistence) {
                FILE -> filePersistenceModule(serialiser)
                MEMORY -> memoryPersistenceModule(serialiser)
                SQLITE -> sqlitePersistenceModule(serialiser)
            }

    private fun filePersistenceModule(serialiser: Serialiser) = FilePersistence(
            decorators,
            serialiser
    )

    private fun memoryPersistenceModule(serialiser: Serialiser) = MemoryPersistence(
            decorators,
            serialiser
    )

    private fun sqlitePersistenceModule(serialiser: Serialiser) = SqlitePersistence(
            decorators,
            serialiser
    )

    enum class PersistenceType {
        FILE,
        MEMORY,
        SQLITE
    }

    private fun <E> dejaVuBuilder(
            persistence: PersistenceType,
            serialiserType: SerialiserType,
            errorFactoryType: ErrorFactoryType<E>
    ) where E : Throwable,
            E : NetworkErrorPredicate =
            DejaVu.builder(
                    context,
                    errorFactoryType.errorFactory,
                    persistenceModuleProvider(
                            persistence,
                            serialiserType.serialiser
                    ),
                    logger
            )

    private fun <E> dejaVuRetrofit(
            persistence: PersistenceType,
            serialiserType: SerialiserType,
            errorFactoryType: ErrorFactoryType<E>
    ) where E : Throwable,
            E : NetworkErrorPredicate =
            dejaVuBuilder(
                    persistence,
                    serialiserType,
                    errorFactoryType
            ).extend(DejaVuRetrofit.extension<E>()).build()

    private fun <E> dejaVuVolley(
            persistence: PersistenceType,
            serialiserType: SerialiserType,
            errorFactoryType: ErrorFactoryType<E>
    ) where E : Throwable,
            E : NetworkErrorPredicate =
            dejaVuBuilder(
                    persistence,
                    serialiserType,
                    errorFactoryType
            ).extend(DejaVuVolley.extension<E>()).build()

    fun <E> createRetrofit(
            persistence: PersistenceType,
            serialiserType: SerialiserType,
            errorFactoryType: ErrorFactoryType<E>
    ) where E : Throwable,
            E : NetworkErrorPredicate =
            DejaVuRetrofitClient(
                    dejaVuRetrofit(
                            persistence,
                            serialiserType,
                            errorFactoryType
                    ),
                    logger
            )

    fun <E> createVolley(
            persistence: PersistenceType,
            serialiserType: SerialiserType,
            errorFactoryType: ErrorFactoryType<E>
    ) where E : Throwable,
            E : NetworkErrorPredicate =
            DejaVuVolleyClient(
                    dejaVuVolley(
                            persistence,
                            serialiserType,
                            errorFactoryType
                    ),
                    logger
            )
}