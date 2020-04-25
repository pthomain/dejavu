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

import dagger.Module
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.persistence.base.store.KeySerialiser
import dev.pthomain.android.dejavu.persistence.base.store.KeyValuePersistenceManager
import dev.pthomain.android.dejavu.persistence.di.PersistenceModule
import dev.pthomain.android.dejavu.persistence.file.FilePersistenceManagerFactory
import dev.pthomain.android.dejavu.persistence.file.FileSerialisationDecorator
import dev.pthomain.android.dejavu.persistence.file.FileStore
import dev.pthomain.android.dejavu.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.shared.PersistenceManager
import dev.pthomain.android.dejavu.shared.utils.Function1
import java.io.File
import java.util.*
import javax.inject.Singleton

@Module(includes = [PersistenceModule::class])
internal class FilePersistenceModule(
        private val logger: Logger,
        private val dateFactory: (Long?) -> Date,
        private val cacheDirectory: File
) {

    @Provides
    @Singleton
    internal fun provideFileStoreFactory(keySerialiser: KeySerialiser) =
            FileStore.Factory(
                    logger,
                    keySerialiser
            )

    @Provides
    internal fun provideFilePersistenceManager(
            fileStoreFactory: FileStore.Factory,
            keySerialiser: KeySerialiser,
            serialisationManager: SerialisationManager
    ): PersistenceManager =
            KeyValuePersistenceManager(
                    dateFactory,
                    logger,
                    keySerialiser,
                    fileStoreFactory.create(cacheDirectory),
                    serialisationManager
            )

    @Provides
    @Singleton
    internal fun provideFileSerialisationDecorator(
            byteToStringConverter: Function1<ByteArray, String>
    ) =
            FileSerialisationDecorator(byteToStringConverter::get)

    @Provides
    @Singleton
    internal fun provideFilePersistenceManagerFactory(
            keySerialiser: KeySerialiser,
            storeFactory: FileStore.Factory,
            serialisationManager: SerialisationManager
    ) =
            FilePersistenceManagerFactory(
                    dateFactory,
                    logger,
                    keySerialiser,
                    storeFactory,
                    serialisationManager
            )

}