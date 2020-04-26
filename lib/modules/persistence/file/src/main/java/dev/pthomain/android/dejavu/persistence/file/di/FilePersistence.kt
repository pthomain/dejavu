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
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.persistence.base.store.KeySerialiser
import dev.pthomain.android.dejavu.persistence.di.PersistenceComponent
import dev.pthomain.android.dejavu.persistence.di.PersistenceModule
import dev.pthomain.android.dejavu.persistence.file.FilePersistenceManagerFactory
import dev.pthomain.android.dejavu.persistence.file.FileSerialisationDecorator
import dev.pthomain.android.dejavu.persistence.file.FileStore
import dev.pthomain.android.dejavu.persistence.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.persistence.serialisation.Serialiser
import dev.pthomain.android.dejavu.shared.di.SharedModule
import dev.pthomain.android.dejavu.shared.di.SilentLogger
import dev.pthomain.android.dejavu.shared.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.shared.utils.Function1
import java.util.*
import javax.inject.Singleton

object FilePersistence {

    class Builder(
            context: Context,
            serialiser: Serialiser,
            vararg decorators: SerialisationDecorator,
            logger: Logger = SilentLogger
    ) : Component by DaggerFilePersistence_Component
            .builder()
            .module(Module(
                    decorators.asList(),
                    serialiser
            ))
            .sharedModule(SharedModule(
                    context.applicationContext,
                    logger
            ))
            .build()

    @Singleton
    @dagger.Component(modules = [Module::class])
    interface Component : PersistenceComponent {
        fun persistenceManagerFactory(): FilePersistenceManagerFactory
    }

    @dagger.Module
    internal class Module(
            decoratorList: List<SerialisationDecorator>,
            serialiser: Serialiser
    ) : PersistenceModule(decoratorList, serialiser) {

        @Provides
        @Singleton
        internal fun provideFileStoreFactory(
                logger: Logger,
                keySerialiser: KeySerialiser
        ) =
                FileStore.Factory(
                        logger,
                        keySerialiser
                )

        @Provides
        @Singleton
        internal fun provideFileSerialisationDecorator(
                byteToStringConverter: Function1<ByteArray, String>
        ): SerialisationDecorator =
                FileSerialisationDecorator(byteToStringConverter::get)

        @Provides
        @Singleton
        internal fun provideFilePersistenceManagerFactory(
                logger: Logger,
                dateFactory: Function1<Long?, Date>,
                keySerialiser: KeySerialiser,
                storeFactory: FileStore.Factory,
                serialisationManager: SerialisationManager,
                serialisationDecorator: SerialisationDecorator
        ) =
                FilePersistenceManagerFactory(
                        dateFactory::get,
                        logger,
                        keySerialiser,
                        storeFactory,
                        serialisationManager,
                        serialisationDecorator
                )

        @Provides
        @Singleton
        internal fun provideFilePersistenceManager(
                context: Context,
                filePersistenceManagerFactory: FilePersistenceManagerFactory
        ) =
                filePersistenceManagerFactory.create(context.cacheDir)

    }
}