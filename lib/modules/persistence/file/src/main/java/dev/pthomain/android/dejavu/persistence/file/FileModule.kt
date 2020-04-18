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

package dev.pthomain.android.dejavu.persistence.file

import dagger.Module
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.DejaVu
import dev.pthomain.android.dejavu.di.Function1
import dev.pthomain.android.dejavu.persistence.PersistenceManager
import dev.pthomain.android.dejavu.persistence.base.store.KeyValuePersistenceManager
import dev.pthomain.android.dejavu.persistence.file.serialisation.FileSerialisationDecorator
import dev.pthomain.android.dejavu.serialisation.FileNameSerialiser
import dev.pthomain.android.dejavu.serialisation.SerialisationManager
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import java.util.*
import javax.inject.Singleton

@Module
abstract class FileModule<E>(
        configuration: DejaVu.Configuration<E>,
        private val cacheDirectory: java.io.File = configuration.context.cacheDir
) where E : Throwable,
        E : NetworkErrorPredicate {

    @Provides
    @Singleton
    internal fun provideFileStoreFactory(
            logger: Logger,
            configuration: DejaVu.Configuration<E>,
            fileNameSerialiser: FileNameSerialiser
    ) =
            FileStore.Factory(
                    logger,
                    configuration,
                    fileNameSerialiser
            )

    @Provides
    internal fun provideFilePersistenceManager(
            fileStoreFactory: FileStore.Factory<E>,
            configuration: DejaVu.Configuration<E>,
            dateFactory: (Long?) -> Date,
            fileNameSerialiser: FileNameSerialiser,
            serialisationManager: SerialisationManager<E>
    ): PersistenceManager<E> =
            KeyValuePersistenceManager(
                    configuration,
                    dateFactory,
                    fileNameSerialiser,
                    fileStoreFactory.create(cacheDirectory),
                    serialisationManager
            )

    @Provides
    @Singleton
    internal fun provideFileSerialisationDecorator(byteToStringConverter: Function1<ByteArray, String>) =
            FileSerialisationDecorator<E>(byteToStringConverter::get)

}