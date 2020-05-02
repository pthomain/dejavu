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

package dev.pthomain.android.dejavu.demo.dejavu

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import com.google.gson.Gson
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.DejaVu
import dev.pthomain.android.dejavu.configuration.error.DejaVuGlitchFactory
import dev.pthomain.android.dejavu.demo.dejavu.DejaVuFactory.Persistence.*
import dev.pthomain.android.dejavu.demo.dejavu.error.CustomApiError
import dev.pthomain.android.dejavu.demo.dejavu.error.CustomApiErrorFactory
import dev.pthomain.android.dejavu.demo.gson.GsonSerialiser
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.dejavu.persistence.file.di.FilePersistence
import dev.pthomain.android.dejavu.persistence.memory.di.MemoryPersistence
import dev.pthomain.android.dejavu.persistence.sqlite.di.SqlitePersistence
import dev.pthomain.android.dejavu.retrofit.DejaVuRetrofit
import dev.pthomain.android.dejavu.serialisation.compression.Compression
import dev.pthomain.android.dejavu.serialisation.encryption.Encryption
import dev.pthomain.android.dejavu.volley.DejaVuVolley
import dev.pthomain.android.glitchy.core.interceptor.error.ErrorFactory
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate
import dev.pthomain.android.glitchy.core.interceptor.error.glitch.Glitch
import dev.pthomain.android.glitchy.core.interceptor.error.glitch.GlitchFactory
import dev.pthomain.android.mumbo.Mumbo

class DejaVuFactory(
        private val uiLogger: Logger,
        private val context: Context
) {

    private val gson = Gson()
    private val serialiser = GsonSerialiser(gson)

    private val compressionDecorator = Compression(uiLogger).serialisationDecorator

    private val encryptionDecorator = with(Mumbo(context, uiLogger)) {
        Encryption(if (SDK_INT >= 23) tink() else conceal()) //FIXME handle UnrecoverableKeyException by clearing the cache
    }.serialisationDecorator

    private fun getDecorators(
            encrypt: Boolean,
            compress: Boolean
    ) = when {
        encrypt && compress -> listOf(compressionDecorator, encryptionDecorator)
        encrypt -> listOf(encryptionDecorator)
        compress -> listOf(compressionDecorator)
        else -> emptyList()
    }

    private fun persistenceModuleProvider(
            encrypt: Boolean,
            compress: Boolean,
            persistence: Persistence
    ): PersistenceManager.ModuleProvider {
        return when (persistence) {
            FILE -> filePersistenceModule(encrypt, compress)
            MEMORY -> memoryPersistenceModule(encrypt, compress)
            SQLITE -> sqlitePersistenceModule(encrypt, compress)
        }
    }

    private fun filePersistenceModule(
            encrypt: Boolean,
            compress: Boolean
    ) = FilePersistence(
            getDecorators(encrypt, compress),
            serialiser
    )

    private fun memoryPersistenceModule(
            encrypt: Boolean,
            compress: Boolean
    ) = MemoryPersistence(
            getDecorators(encrypt, compress),
            serialiser
    )

    private fun sqlitePersistenceModule(
            encrypt: Boolean,
            compress: Boolean
    ) = SqlitePersistence(
            getDecorators(encrypt, compress),
            serialiser
    )

    private fun <E> dejaVuBuilder(
            errorFactoryType: ErrorFactoryType<E>,
            encrypt: Boolean,
            compress: Boolean,
            persistence: Persistence
    ) where E : Throwable,
            E : NetworkErrorPredicate =
            DejaVu.builder(
                    context,
                    errorFactoryType.errorFactory,
                    persistenceModuleProvider(encrypt, compress, persistence),
                    uiLogger
            )

    fun <E> createDejaVuRetrofit(
            encrypt: Boolean,
            compress: Boolean,
            errorFactoryType: ErrorFactoryType<E>,
            persistence: Persistence
    ) where E : Throwable,
            E : NetworkErrorPredicate =
            dejaVuBuilder(
                    errorFactoryType,
                    encrypt,
                    compress,
                    persistence
            )
                    .extend(DejaVuRetrofit.extension<E>())
                    .build()

    fun <E> createDejaVuVolley(
            encrypt: Boolean,
            compress: Boolean,
            errorFactoryType: ErrorFactoryType<E>,
            persistence: Persistence
    ) where E : Throwable,
            E : NetworkErrorPredicate =
            dejaVuBuilder(
                    errorFactoryType,
                    encrypt,
                    compress,
                    persistence
            )
                    .extend(DejaVuVolley.extension<E>())
                    .build()

    enum class Persistence {
        FILE,
        MEMORY,
        SQLITE
    }


    sealed class ErrorFactoryType<E>(
            val errorFactory: ErrorFactory<E>
    ) where E : Throwable,
            E : NetworkErrorPredicate {

        object Default : ErrorFactoryType<Glitch>(DejaVuGlitchFactory(GlitchFactory()))
        object Custom : ErrorFactoryType<CustomApiError>(CustomApiErrorFactory())
    }
}