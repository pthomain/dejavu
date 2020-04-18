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

package dev.pthomain.android.dejavu.persistence.sqlite

import android.content.ContentValues
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dagger.Module
import dagger.Provides
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.DejaVu
import dev.pthomain.android.dejavu.di.Function1
import dev.pthomain.android.dejavu.serialisation.SerialisationManager
import dev.pthomain.android.glitchy.interceptor.error.NetworkErrorPredicate
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import java.util.*
import javax.inject.Singleton

@Module
abstract class SqliteModule<E> where E : Throwable,
                                     E : NetworkErrorPredicate {

    @Provides
    @Singleton
    fun provideDatabasePersistenceManagerFactory(configuration: DejaVu.Configuration<E>,
                                                 database: SupportSQLiteDatabase?,
                                                 dateFactory: Function1<Long?, Date>,
                                                 serialisationManagerFactory: SerialisationManager.Factory<E>) =
            if (database != null)
                DatabasePersistenceManager.Factory(
                        database,
                        serialisationManagerFactory,
                        configuration,
                        dateFactory::get,
                        ::mapToContentValues
                )
            else null

    @Provides
    @Singleton
    fun provideSqlOpenHelperCallback(configuration: DejaVu.Configuration<E>): SupportSQLiteOpenHelper.Callback? =
            SqlOpenHelperCallback(DATABASE_VERSION)

    @Provides
    @Singleton
    @Synchronized
    fun provideDatabase(sqlOpenHelper: SupportSQLiteOpenHelper?) =
            sqlOpenHelper?.writableDatabase

    @Provides
    @Singleton
    fun provideSqlOpenHelper(context: Context,
                             callback: SupportSQLiteOpenHelper.Callback?): SupportSQLiteOpenHelper? =
            callback?.let {
                RequerySQLiteOpenHelperFactory().create(
                        SupportSQLiteOpenHelper.Configuration.builder(context)
                                .name(DATABASE_NAME)
                                .callback(it)
                                .build()
                )
            }

    @Provides
    @Singleton
    fun provideDatabaseStatisticsCompiler(configuration: DejaVu.Configuration<E>,
                                          logger: Logger,
                                          database: SupportSQLiteDatabase,
                                          dateFactory: Function1<Long?, Date>) =
            DatabaseStatisticsCompiler(
                    configuration,
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
