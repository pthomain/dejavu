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
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.DejaVu
import dev.pthomain.android.dejavu.demo.dejavu.DejaVuRetrofitClient
import dev.pthomain.android.dejavu.demo.dejavu.DejaVuVolleyClient
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.DejaVuFactory.PersistenceType.*
import dev.pthomain.android.dejavu.persistence.memory.di.MemoryPersistence
import dev.pthomain.android.dejavu.persistence.sqlite.di.SqlitePersistence
import dev.pthomain.android.dejavu.retrofit.DejaVuRetrofit
import dev.pthomain.android.dejavu.serialisation.Serialiser
import dev.pthomain.android.dejavu.serialisation.encryption.Encryption
import dev.pthomain.android.dejavu.volley.DejaVuVolley
import dev.pthomain.android.glitchy.core.interceptor.error.NetworkErrorPredicate

class DejaVuFactory(
        private val logger: Logger,
        private val context: Context
) {

    private val encryptionDecorator = Encryption(context).serialisationDecorator

    var encrypt = false
    var compress = false

    private val decorators = listOf(encryptionDecorator)

    private fun persistenceModuleProvider(
            persistence: PersistenceType,
            serialiser: Serialiser
    ) =
            when (persistence) {
                MEMORY -> memoryPersistenceModule(serialiser)
                SQLITE -> sqlitePersistenceModule(serialiser)
            }

    private fun memoryPersistenceModule(serialiser: Serialiser) = MemoryPersistence(
            decorators,
            serialiser
    )

    private fun sqlitePersistenceModule(serialiser: Serialiser) = SqlitePersistence(
            context,
            decorators,
            serialiser,
            logger,
            TODO("dateFactory needed")
    )

    enum class PersistenceType {
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
