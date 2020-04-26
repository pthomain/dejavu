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

package dev.pthomain.android.dejavu.persistence.memory.di

import android.content.Context
import androidx.collection.LruCache
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.persistence.base.store.KeySerialiser
import dev.pthomain.android.dejavu.persistence.base.store.KeyValuePersistenceManager
import dev.pthomain.android.dejavu.persistence.di.PersistenceComponent
import dev.pthomain.android.dejavu.persistence.di.PersistenceModule
import dev.pthomain.android.dejavu.persistence.memory.MemoryPersistenceManagerFactory
import dev.pthomain.android.dejavu.persistence.memory.MemoryStore
import dev.pthomain.android.dejavu.persistence.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.persistence.serialisation.Serialiser
import dev.pthomain.android.dejavu.shared.PersistenceManager
import dev.pthomain.android.dejavu.shared.di.SharedModule
import dev.pthomain.android.dejavu.shared.di.SilentLogger
import dev.pthomain.android.dejavu.shared.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.shared.utils.Function1
import java.util.*
import javax.inject.Singleton

object MemoryPersistence {

    class Builder(
            context: Context,
            serialiser: Serialiser,
            vararg decorators: SerialisationDecorator,
            maxEntries: Int = 20,
            logger: Logger = SilentLogger
    ) : Component by DaggerMemoryPersistence_Component
            .builder()
            .sharedModule(SharedModule(
                    context.applicationContext,
                    logger
            ))
            .module(Module(
                    decorators.asList(),
                    serialiser,
                    maxEntries
            ))
            .build()

    @Singleton
    @dagger.Component(modules = [Module::class])
    internal interface Component : PersistenceComponent {
        fun persistenceManagerFactory(): MemoryPersistenceManagerFactory
    }

    @dagger.Module
    internal class Module(
            decoratorList: List<SerialisationDecorator>,
            serialiser: Serialiser,
            private val maxEntries: Int
    ) : PersistenceModule(decoratorList, serialiser) {

        @Provides
        @Singleton
        internal fun provideMemoryPersistenceManagerFactory(
                dateFactory: Function1<Long?, Date>,
                logger: Logger,
                keySerialiser: KeySerialiser,
                storeFactory: MemoryStore.Factory,
                serialisationManager: SerialisationManager
        ) =
                MemoryPersistenceManagerFactory(
                        dateFactory::get,
                        logger,
                        keySerialiser,
                        storeFactory,
                        serialisationManager
                )

        @Provides
        @Singleton
        internal fun provideMemoryStoreFactory() =
                MemoryStore.Factory { LruCache(it) }

        @Provides
        internal fun provideMemoryPersistenceManager(
                dateFactory: Function1<Long?, Date>,
                logger: Logger,
                memoryStoreFactory: MemoryStore.Factory,
                serialisationManager: SerialisationManager,
                keySerialiser: KeySerialiser
        ): PersistenceManager =
                KeyValuePersistenceManager(
                        dateFactory::get,
                        logger,
                        keySerialiser,
                        memoryStoreFactory.create(maxEntries),
                        serialisationManager
                )
    }
}