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
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dev.pthomain.android.dejavu.persistence.di.PersistenceModule
import dev.pthomain.android.dejavu.persistence.serialisation.Serialiser
import dev.pthomain.android.dejavu.persistence.sqlite.DatabasePersistenceManager
import dev.pthomain.android.dejavu.persistence.sqlite.DatabaseStatisticsCompiler
import dev.pthomain.android.dejavu.persistence.sqlite.SqlOpenHelperCallback
import dev.pthomain.android.dejavu.shared.persistence.PersistenceManager
import dev.pthomain.android.dejavu.shared.serialisation.SerialisationDecorator
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import org.koin.core.qualifier.named
import org.koin.dsl.module

class SqlitePersistence(
        decoratorList: List<SerialisationDecorator>,
        serialiser: Serialiser
) : PersistenceManager.ModuleProvider {

    private val persistenceModule = PersistenceModule(decoratorList, serialiser).module

    override val modules = persistenceModule + module {

        single<PersistenceManager> {
            DatabasePersistenceManager(
                    get(),
                    get(),
                    get(),
                    get(named("dateFactory")),
                    ::mapToContentValues
            )
        }
        single { SqlOpenHelperCallback(DATABASE_VERSION) }

        single { get<SupportSQLiteOpenHelper>().writableDatabase }

        single {
            RequerySQLiteOpenHelperFactory().create(
                    SupportSQLiteOpenHelper.Configuration.builder(get())
                            .name(DATABASE_NAME)
                            .callback(get())
                            .build()
            )
        }

        single {
            DatabaseStatisticsCompiler(
                    get(),
                    get(named("dateFactory")),
                    get()
            )
        }

    }
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
