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

import androidx.collection.LruCache
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.dejavu.persistence.base.store.KeyValuePersistenceManager
import dev.pthomain.android.dejavu.persistence.di.PersistenceModule
import dev.pthomain.android.dejavu.persistence.memory.MemoryPersistenceManagerFactory
import dev.pthomain.android.dejavu.persistence.memory.MemoryStore
import dev.pthomain.android.dejavu.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.serialisation.Serialiser
import org.koin.core.qualifier.named
import org.koin.dsl.module

class MemoryPersistence(
        override val decorators: List<SerialisationDecorator>,
        serialiser: Serialiser,
        private val maxEntries: Int = 20
) : PersistenceManager.ModuleProvider {

    private val persistenceModule = PersistenceModule(decorators, serialiser).module

    override val modules = persistenceModule + module {

        single {
            MemoryPersistenceManagerFactory(
                    get(named("dateFactory")),
                    get(),
                    get(),
                    get(),
                    get()
            )
        }

        single {
            MemoryStore.Factory(
                    ::LruCache,
                    get(named("dateFactory"))
            )
        }

        single<PersistenceManager> {
            KeyValuePersistenceManager(
                    get(named("dateFactory")),
                    get(),
                    get(),
                    get<MemoryStore.Factory>().create(maxEntries),
                    get()
            )
        }
    }
}