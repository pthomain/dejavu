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

package dev.pthomain.android.dejavu.injection.integration.component

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dagger.Component
import dev.pthomain.android.dejavu.configuration.Serialiser
import dev.pthomain.android.dejavu.injection.DejaVuComponent
import dev.pthomain.android.dejavu.injection.Function1
import dev.pthomain.android.dejavu.injection.Function3
import dev.pthomain.android.dejavu.injection.integration.module.IntegrationDejaVuModule
import dev.pthomain.android.dejavu.interceptors.RxType
import dev.pthomain.android.dejavu.interceptors.cache.CacheInterceptor
import dev.pthomain.android.dejavu.interceptors.cache.CacheManager
import dev.pthomain.android.dejavu.interceptors.cache.metadata.CacheMetadata
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.persistence.PersistenceManagerFactory
import dev.pthomain.android.dejavu.interceptors.cache.persistence.base.KeyValuePersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.database.DatabasePersistenceManager
import dev.pthomain.android.dejavu.interceptors.cache.persistence.file.FileStore
import dev.pthomain.android.dejavu.interceptors.cache.persistence.memory.MemoryStore
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.interceptors.error.ErrorInterceptor
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.interceptors.response.EmptyResponseWrapperFactory
import dev.pthomain.android.dejavu.interceptors.response.ResponseInterceptor
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor
import dev.pthomain.android.mumbo.base.EncryptionManager
import io.reactivex.subjects.PublishSubject
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.util.*
import javax.inject.Singleton

@Singleton
@Component(modules = [IntegrationDejaVuModule::class])
internal interface IntegrationDejaVuComponent : DejaVuComponent<Glitch> {

    fun dateFactory(): Function1<Long?, Date>
    fun serialiser(): Serialiser
    fun encryptionManager(): EncryptionManager
    fun sqlOpenHelperCallback(): SupportSQLiteOpenHelper.Callback?
    fun sqlOpenHelper(): SupportSQLiteOpenHelper?
    fun database(): SupportSQLiteDatabase?
    fun hasher(): Hasher
    fun serialisationManagerFactory(): SerialisationManager.Factory<Glitch>
    fun databasePersistenceManagerFactory(): DatabasePersistenceManager.Factory<Glitch>?
    fun filePersistenceManagerFactory(): KeyValuePersistenceManager.FileFactory<Glitch>
    fun fileStoreFactory(): FileStore.Factory<Glitch>
    fun memoryPersistenceManagerFactory(): KeyValuePersistenceManager.MemoryFactory<Glitch>
    fun memoryStoreFactory(): MemoryStore.Factory
    fun cacheManager(): CacheManager<Glitch>
    fun errorInterceptorFactory(): Function1<CacheToken, ErrorInterceptor<Glitch>>
    fun cacheInterceptorFactory(): Function3<ErrorInterceptor<Glitch>, CacheToken, Long, CacheInterceptor<Glitch>>
    fun responseInterceptorFactory(): Function3<CacheToken, RxType, Long, ResponseInterceptor<Glitch>>
    fun defaultAdapterFactory(): RxJava2CallAdapterFactory
    fun cacheMetadataSubject(): PublishSubject<CacheMetadata<Glitch>>
    fun annotationProcessor(): AnnotationProcessor<Glitch>
    fun emptyResponseFactory(): EmptyResponseWrapperFactory<Glitch>
    fun supportSQLiteOpenHelper(): SupportSQLiteOpenHelper?
    fun persistenceManagerFactory(): PersistenceManagerFactory<Glitch>

}
