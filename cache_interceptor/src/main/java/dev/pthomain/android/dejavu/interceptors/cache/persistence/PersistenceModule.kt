/*
 *
 *  Copyright (C) 2017 Pierre Thomain
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

package dev.pthomain.android.dejavu.interceptors.cache.persistence

import android.util.LruCache
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dagger.Module
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.configuration.error.NetworkErrorPredicate
import dev.pthomain.android.dejavu.injection.Function1
import dev.pthomain.android.dejavu.injection.mapToContentValues
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.KeyValuePersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.database.DatabasePersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.database.SqlOpenHelperCallback
import dev.pthomain.android.dejavu.interceptors.cache.persistence.file.FileStore
import dev.pthomain.android.dejavu.interceptors.cache.persistence.memory.MemoryStore
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.FileNameSerialiser
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager
import java.util.*
import javax.inject.Singleton

@Module
internal abstract class PersistenceModule<E> where E : Exception,
                                                   E : NetworkErrorPredicate {

    @Provides
    @Singleton
    fun providePersistenceManagerFactory(databasePersistenceManagerFactory: DatabasePersistenceManager.Factory<E>?,
                                         filePersistenceManagerFactory: KeyValuePersistenceManager.FileFactory<E>,
                                         memoryPersistenceManagerFactory: KeyValuePersistenceManager.MemoryFactory<E>) =
            PersistenceManagerFactory(
                    filePersistenceManagerFactory,
                    databasePersistenceManagerFactory,
                    memoryPersistenceManagerFactory
            )

    @Provides
    @Singleton
    fun providePersistenceManager(configuration: DejaVuConfiguration<E>,
                                  persistenceManagerFactory: PersistenceManagerFactory<E>,
                                  databasePersistenceManagerFactory: DatabasePersistenceManager.Factory<E>?): PersistenceManager<E> =
            configuration.persistenceManagerPicker
                    ?.invoke(persistenceManagerFactory)
                    ?: databasePersistenceManagerFactory!!.create()

    @Provides
    @Singleton
    fun provideFileStoreFactory(logger: Logger,
                                dejaVuConfiguration: DejaVuConfiguration<E>,
                                fileNameSerialiser: FileNameSerialiser) =
            FileStore.Factory(
                    logger,
                    dejaVuConfiguration,
                    fileNameSerialiser
            )

    @Provides
    @Singleton
    fun provideMemoryStoreFactory() =
            MemoryStore.Factory(::LruCache)

    @Provides
    @Singleton
    fun provideMemoryPersistenceManagerFactory(configuration: DejaVuConfiguration<E>,
                                               fileStoreFactory: FileStore.Factory<E>,
                                               hasher: Hasher,
                                               serialisationManagerFactory: SerialisationManager.Factory<E>,
                                               dateFactory: Function1<Long?, Date>,
                                               fileNameSerialiser: FileNameSerialiser) =
            KeyValuePersistenceManager.FileFactory(
                    fileStoreFactory,
                    hasher,
                    serialisationManagerFactory,
                    configuration,
                    dateFactory::get,
                    fileNameSerialiser
            )

    @Provides
    @Singleton
    fun provideFilePersistenceManagerFactory(configuration: DejaVuConfiguration<E>,
                                             memoryStoreFactory: MemoryStore.Factory,
                                             hasher: Hasher,
                                             serialisationManagerFactory: SerialisationManager.Factory<E>,
                                             dateFactory: Function1<Long?, Date>,
                                             fileNameSerialiser: FileNameSerialiser) =
            KeyValuePersistenceManager.MemoryFactory(
                    memoryStoreFactory,
                    hasher,
                    serialisationManagerFactory,
                    configuration,
                    dateFactory::get,
                    fileNameSerialiser
            )

    @Provides
    @Singleton
    fun provideDatabasePersistenceManagerFactory(configuration: DejaVuConfiguration<E>,
                                                 hasher: Hasher,
                                                 database: SupportSQLiteDatabase?,
                                                 dateFactory: Function1<Long?, Date>,
                                                 serialisationManagerFactory: SerialisationManager.Factory<E>) =
            if (database != null)
                DatabasePersistenceManager.Factory(
                        database,
                        hasher,
                        serialisationManagerFactory,
                        configuration,
                        dateFactory::get,
                        ::mapToContentValues
                )
            else null

    @Provides
    @Singleton
    fun provideSqlOpenHelperCallback(configuration: DejaVuConfiguration<E>): SupportSQLiteOpenHelper.Callback? =
            if (configuration.useDatabase) SqlOpenHelperCallback(DATABASE_VERSION)
            else null

    @Provides
    @Singleton
    @Synchronized
    fun provideDatabase(sqlOpenHelper: SupportSQLiteOpenHelper?) =
            sqlOpenHelper?.writableDatabase

    companion object {
        const val DATABASE_NAME = "dejavu.db"
        const val DATABASE_VERSION = 1
    }
}