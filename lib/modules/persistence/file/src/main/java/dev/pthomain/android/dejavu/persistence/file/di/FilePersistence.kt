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
import dev.pthomain.android.dejavu.persistence.di.PersistenceModule
import dev.pthomain.android.dejavu.persistence.file.FilePersistenceManagerFactory
import dev.pthomain.android.dejavu.persistence.file.FileSerialisationDecorator
import dev.pthomain.android.dejavu.persistence.file.FileStore
import dev.pthomain.android.dejavu.persistence.serialisation.Serialiser
import dev.pthomain.android.dejavu.shared.persistence.PersistenceManager
import dev.pthomain.android.dejavu.shared.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.shared.utils.Function1
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.*

class FilePersistence(
        decoratorList: List<SerialisationDecorator>,
        serialiser: Serialiser
) : PersistenceManager.ModuleProvider {

    private val persistenceModule = PersistenceModule(decoratorList, serialiser).module

    override val modules = persistenceModule + module {

        single {
            FileStore.Factory(
                    get(),
                    get()
            )
        }

        single<SerialisationDecorator> {
            FileSerialisationDecorator(::String)
        }

        single {
            FilePersistenceManagerFactory(
                    get<Function1<Long?, Date>>(named("dateFactory"))::get,
                    get(),
                    get(),
                    get(),
                    get(),
                    get()
            )
        }

        single {
            get<FilePersistenceManagerFactory>().create(get<Context>().cacheDir)
        }

    }
}