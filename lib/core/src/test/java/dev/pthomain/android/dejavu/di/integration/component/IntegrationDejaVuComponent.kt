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

package dev.pthomain.android.dejavu.di.integration.component

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dagger.Component
import dev.pthomain.android.dejavu.cache.CacheManager
import dev.pthomain.android.dejavu.di.integration.module.IntegrationModule
import dev.pthomain.android.dejavu.interceptors.CacheInterceptor
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseFactory
import dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
import dev.pthomain.android.dejavu.persistence.base.store.KeyValuePersistenceManager
import dev.pthomain.android.dejavu.persistence.file.FileStore
import dev.pthomain.android.dejavu.persistence.memory.MemoryStore
import dev.pthomain.android.dejavu.persistence.sqlite.DatabasePersistenceManager
import dev.pthomain.android.dejavu.retrofit.annotations.processor.AnnotationProcessor
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.Hasher
import dev.pthomain.android.dejavu.shared.utils.Function1
import dev.pthomain.android.glitchy.interceptor.error.glitch.Glitch
import dev.pthomain.android.mumbo.base.EncryptionManager
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.util.*
import javax.inject.Singleton

@Singleton
@Component(modules = [IntegrationModule::class])
internal interface IntegrationDejaVuComponent : DejaVuComponent<Glitch> {

    fun dateFactory(): Function1<Long?, Date>
    fun serialiser(): dev.pthomain.android.dejavu.serialisation.Serialiser
    fun encryptionManager(): EncryptionManager?
    fun sqlOpenHelperCallback(): SupportSQLiteOpenHelper.Callback?
    fun sqlOpenHelper(): SupportSQLiteOpenHelper?
    fun database(): SupportSQLiteDatabase?
    fun hasher(): Hasher
    fun serialisationManagerFactory(): dev.pthomain.android.dejavu.serialisation.SerialisationManager.Factory<Glitch>
    fun databasePersistenceManagerFactory(): dev.pthomain.android.dejavu.persistence.sqlite.DatabasePersistenceManager.Factory<Glitch>?
    fun filePersistenceManagerFactory(): KeyValuePersistenceManager.FileFactory<Glitch>
    fun fileStoreFactory(): dev.pthomain.android.dejavu.persistence.file.FileStore.Factory<Glitch>
    fun memoryPersistenceManagerFactory(): KeyValuePersistenceManager.MemoryFactory<Glitch>
    fun memoryStoreFactory(): dev.pthomain.android.dejavu.persistence.memory.MemoryStore.Factory
    fun cacheManager(): CacheManager<Glitch>
    fun cacheInterceptorFactory(): CacheInterceptor.Factory<Glitch>
    fun responseInterceptorFactory(): ResponseInterceptor.Factory<Glitch>
    fun defaultAdapterFactory(): RxJava2CallAdapterFactory
    fun annotationProcessor(): AnnotationProcessor<Glitch>
    fun emptyResponseFactory(): EmptyResponseFactory<Glitch>
    fun supportSQLiteOpenHelper(): SupportSQLiteOpenHelper?
    fun persistenceManagerFactory(): PersistenceManagerFactory<Glitch>

}
