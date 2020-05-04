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
import dev.pthomain.android.dejavu.DejaVu
import dev.pthomain.android.dejavu.demo.dejavu.DejaVuRetrofitClient
import dev.pthomain.android.dejavu.demo.dejavu.DejaVuVolleyClient
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.DejaVuFactory.PersistenceType.FILE
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.DejaVuFactory.PersistenceType.SQLITE
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.DejaVuFactory.PersistenceType.MEMORY
import dev.pthomain.android.dejavu.serialisation.gson.GsonSerialiser
import dev.pthomain.android.dejavu.persistence.file.di.FilePersistence
import dev.pthomain.android.dejavu.persistence.memory.di.MemoryPersistence
import dev.pthomain.android.dejavu.persistence.sqlite.di.SqlitePersistence
import dev.pthomain.android.dejavu.retrofit.DejaVuRetrofit
import dev.pthomain.android.dejavu.serialisation.compression.Compression
import dev.pthomain.android.dejavu.serialisation.encryption.Encryption
import dev.pthomain.android.dejavu.volley.DejaVuVolley
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.mumbo.Mumbo

class DejaVuFactory(
        private val logger: Logger,
        private val context: Context
) {

    private val gson = Gson()
    private val serialiser = GsonSerialiser(gson)

    private val compressionDecorator = Compression(logger).serialisationDecorator

    private val encryptionDecorator = with(Mumbo(context, logger)) {
        Encryption(if (SDK_INT >= 23) tink() else conceal()) //FIXME handle UnrecoverableKeyException by clearing the cache
    }.serialisationDecorator

    var persistence = FILE
    var encrypt = false
    var compress = false

    private fun getDecorators(
            encrypt: Boolean,
            compress: Boolean
    ) = when {
        encrypt && compress -> listOf(compressionDecorator, encryptionDecorator)
        encrypt -> listOf(encryptionDecorator)
        compress -> listOf(compressionDecorator)
        else -> emptyList()
    }

    private fun persistenceModuleProvider() = when (persistence) {
        FILE -> filePersistenceModule()
        MEMORY -> memoryPersistenceModule()
        SQLITE -> sqlitePersistenceModule()
    }

    private fun filePersistenceModule() = FilePersistence(
            getDecorators(encrypt, compress),
            serialiser
    )

    private fun memoryPersistenceModule() = MemoryPersistence(
            getDecorators(encrypt, compress),
            serialiser
    )

    private fun sqlitePersistenceModule() = SqlitePersistence(
            getDecorators(encrypt, compress),
            serialiser
    )

    enum class PersistenceType {
        FILE,
        MEMORY,
        SQLITE
    }

    private fun <E> dejaVuBuilder(errorFactoryType: ErrorFactoryType<E>)
            where E : Throwable,
                  E : NetworkErrorPredicate =
            DejaVu.builder(
                    context,
                    errorFactoryType.errorFactory,
                    persistenceModuleProvider(),
                    logger
            )

    private fun <E> dejaVuRetrofit(errorFactoryType: ErrorFactoryType<E>)
            where E : Throwable,
                  E : NetworkErrorPredicate =
            dejaVuBuilder(errorFactoryType)
                    .extend(DejaVuRetrofit.extension<E>()).build()

    private fun <E> dejaVuVolley(errorFactoryType: ErrorFactoryType<E>)
            where E : Throwable,
                  E : NetworkErrorPredicate =
            dejaVuBuilder(errorFactoryType)
                    .extend(DejaVuVolley.extension<E>()).build()

    fun <E> createRetrofit(errorFactoryType: ErrorFactoryType<E>)
            where E : Throwable,
                  E : NetworkErrorPredicate =
            DejaVuRetrofitClient(
                    dejaVuRetrofit(errorFactoryType),
                    logger
            )

    fun <E> createVolley(errorFactoryType: ErrorFactoryType<E>)
            where E : Throwable,
                  E : NetworkErrorPredicate =
            DejaVuVolleyClient(
                    dejaVuVolley(errorFactoryType),
                    logger
            )
}