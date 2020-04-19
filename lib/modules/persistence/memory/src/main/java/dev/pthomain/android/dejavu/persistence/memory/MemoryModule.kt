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

package dev.pthomain.android.dejavu.persistence.memory

import androidx.collection.LruCache
import dagger.Module
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.DejaVu.Configuration
import dev.pthomain.android.dejavu.configuration.PersistenceManager
import dev.pthomain.android.dejavu.persistence.base.store.KeySerialiser
import dev.pthomain.android.dejavu.persistence.base.store.KeyValuePersistenceManager
import dev.pthomain.android.dejavu.serialisation.SerialisationManager
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import java.util.*
import javax.inject.Singleton

@Module
abstract class MemoryModule<E>(private val maxEntries: Int = 20)
        where E : Throwable,
              E : NetworkErrorPredicate {

    @Provides
    @Singleton
    internal fun provideMemoryStoreFactory() =
            MemoryStore.Factory { LruCache(it) }

    @Provides
    internal fun provideMemoryPersistenceManager(
            memoryStoreFactory: MemoryStore.Factory,
            serialisationManager: SerialisationManager<E>,
            configuration: Configuration<E>,
            logger: Logger,
            dateFactory: (Long?) -> Date,
            keySerialiser: KeySerialiser
    ): PersistenceManager<E> =
            KeyValuePersistenceManager(
                    configuration,
                    dateFactory,
                    logger,
                    keySerialiser,
                    memoryStoreFactory.create(maxEntries),
                    serialisationManager
            )
}