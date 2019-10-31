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

package dev.pthomain.android.dejavu.interceptors.cache.persistence.statistics

import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.injection.Function1
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.statistics.database.DatabaseStatisticsCompiler
import dev.pthomain.android.dejavu.interceptors.cache.persistence.statistics.file.FileStatisticsCompiler
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.FileNameSerialiser
import dev.pthomain.android.dejavu.interceptors.error.error.NetworkErrorPredicate
import java.util.*
import javax.inject.Singleton

@Module
internal abstract class StatisticsModule<E>
        where E : Exception,
              E : NetworkErrorPredicate {

    @Provides
    @Singleton
    fun provideDatabaseStatisticsCompiler(configuration: DejaVuConfiguration<E>,
                                          logger: Logger,
                                          database: SupportSQLiteDatabase?,
                                          dateFactory: Function1<Long?, Date>) =
            database?.let {
                DatabaseStatisticsCompiler(
                        configuration,
                        logger,
                        dateFactory::get,
                        it
                )
            }

    @Provides
    @Singleton
    fun provideFileStatisticsCompiler(fileNameSerialiser: FileNameSerialiser,
                                      persistenceManager: PersistenceManager<E>,
                                      dateFactory: Function1<Long?, Date>) =
            null as FileStatisticsCompiler? //FIXME
//            (persistenceManager as? FilePersistenceManager<E>)?.let {
//                FileStatisticsCompiler(
//                        configuration,
//                        it.cacheDirectory,
//                        ::File,
//                        { BufferedInputStream(FileInputStream(it)) },
//                        dateFactory::get,
//                        fileNameSerialiser
//                )
//            }

    @Provides
    @Singleton
    fun provideStatisticsCompiler(fileStatisticsCompiler: FileStatisticsCompiler?,
                                  databaseStatisticsCompiler: DatabaseStatisticsCompiler?): StatisticsCompiler =
            databaseStatisticsCompiler ?: fileStatisticsCompiler!!

}
