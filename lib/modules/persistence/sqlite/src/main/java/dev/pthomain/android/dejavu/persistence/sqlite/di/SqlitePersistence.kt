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

package dev.pthomain.android.dejavu.persistence.sqlite.di

import android.content.ContentValues
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.persistence.di.PersistenceComponent
import dev.pthomain.android.dejavu.persistence.di.PersistenceModule
import dev.pthomain.android.dejavu.persistence.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.persistence.serialisation.Serialiser
import dev.pthomain.android.dejavu.persistence.sqlite.DatabasePersistenceManager
import dev.pthomain.android.dejavu.persistence.sqlite.DatabaseStatisticsCompiler
import dev.pthomain.android.dejavu.persistence.sqlite.SqlOpenHelperCallback
import dev.pthomain.android.dejavu.shared.PersistenceManager
import dev.pthomain.android.dejavu.shared.di.SharedModule
import dev.pthomain.android.dejavu.shared.di.SilentLogger
import dev.pthomain.android.dejavu.shared.serialisation.SerialisationDecorator
import dev.pthomain.android.dejavu.shared.utils.Function1
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import java.util.*
import javax.inject.Singleton

object SqlitePersistence {

    class Builder(
            context: Context,
            serialiser: Serialiser,
            vararg decorators: SerialisationDecorator,
            logger: Logger = SilentLogger
    ) : Component by DaggerSqlitePersistence_Component
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
    internal interface Component : PersistenceComponent

    @dagger.Module
    internal class Module(
            decoratorList: List<SerialisationDecorator>,
            serialiser: Serialiser
    ) : PersistenceModule(decoratorList, serialiser) {

        @Provides
        @Singleton
        internal fun provideDatabasePersistenceManagerFactory(
                logger: Logger,
                database: SupportSQLiteDatabase,
                dateFactory: Function1<Long?, Date>,
                serialisationManager: SerialisationManager
        ): PersistenceManager =
                DatabasePersistenceManager(
                        database,
                        logger,
                        serialisationManager,
                        dateFactory::get,
                        ::mapToContentValues
                )

        @Provides
        @Singleton
        internal fun provideSqlOpenHelperCallback(): SupportSQLiteOpenHelper.Callback =
                SqlOpenHelperCallback(DATABASE_VERSION)

        @Provides
        @Singleton
        @Synchronized
        internal fun provideDatabase(sqlOpenHelper: SupportSQLiteOpenHelper) =
                sqlOpenHelper.writableDatabase

        @Provides
        @Singleton
        internal fun provideSqlOpenHelper(
                context: Context,
                callback: SupportSQLiteOpenHelper.Callback
        ): SupportSQLiteOpenHelper =
                RequerySQLiteOpenHelperFactory().create(
                        SupportSQLiteOpenHelper.Configuration.builder(context)
                                .name(DATABASE_NAME)
                                .callback(callback)
                                .build()
                )

        @Provides
        @Singleton
        internal fun provideDatabaseStatisticsCompiler(
                logger: Logger,
                database: SupportSQLiteDatabase,
                dateFactory: Function1<Long?, Date>
        ) =
                DatabaseStatisticsCompiler(
                        logger,
                        dateFactory::get,
                        database
                )

    }

    internal const val DATABASE_NAME = "dejavu.db"
    internal const val DATABASE_VERSION = 1

    internal fun mapToContentValues(map: Map<String, *>) = ContentValues().apply {
        for ((key, value) in map) {
            when (value) {
                is Boolean -> put(key, value)
                is Float -> put(key, value)
                is Double -> put(key, value)
                is Long -> put(key, value)
                is Int -> put(key, value)
                is Byte -> put(key, value)
                is ByteArray -> put(key, value)
                is Short -> put(key, value)
                is String -> put(key, value)
            }
        }
    }
}
