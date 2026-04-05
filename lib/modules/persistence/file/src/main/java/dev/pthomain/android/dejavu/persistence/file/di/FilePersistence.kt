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

package dev.pthomain.android.dejavu.persistence.file.di

import android.content.Context
import dev.pthomain.android.dejavu.utils.Logger
import dev.pthomain.android.dejavu.di.DateFactory
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.dejavu.persistence.di.PersistenceModule
import dev.pthomain.android.dejavu.persistence.file.FilePersistenceManagerFactory
import dev.pthomain.android.dejavu.persistence.file.FileStore
import dev.pthomain.android.dejavu.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.serialisation.Serialiser

class FilePersistence(
        override val decorators: List<SerialisationDecorator>,
        private val serialiser: Serialiser
) : PersistenceManager.ComponentProvider {

    override fun create(
            context: Context,
            dateFactory: DateFactory,
            logger: Logger
    ): PersistenceManager {
        val persistenceModule = PersistenceModule(decorators, serialiser)
        val serialisationManager = persistenceModule.createSerialisationManager()
        val keySerialiser = persistenceModule.createKeySerialiser(dateFactory)

        val storeFactory = FileStore.Factory(logger, keySerialiser)
        val filePersistenceManagerFactory = FilePersistenceManagerFactory(
                dateFactory,
                logger,
                keySerialiser,
                storeFactory,
                serialisationManager
        )

        return filePersistenceManagerFactory.create(context.cacheDir)
    }
}
