/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.dejavu.injection.unit

import android.content.ContentValues
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.nhaarman.mockitokotlin2.mock
import dagger.Module
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.requery.android.database.sqlite.SQLiteDatabase
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.injection.module.CacheModule
import uk.co.glass_software.android.dejavu.interceptors.DejaVuInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.CacheInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.CacheManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.DatabaseManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.GsonSerialiser
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.interceptors.internal.error.ErrorInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.response.EmptyResponseFactory
import uk.co.glass_software.android.dejavu.interceptors.internal.response.ResponseInterceptor
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.retrofit.ProcessingErrorAdapter
import uk.co.glass_software.android.dejavu.retrofit.RetrofitCallAdapterFactory
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor
import uk.co.glass_software.android.shared_preferences.StoreEntryFactory
import uk.co.glass_software.android.shared_preferences.encryption.manager.EncryptionManager
import java.util.*

@Module
internal class UnitTestCacheModule
    : CacheModule<Glitch> {

    override fun provideContext(): Context = mock()

    override fun provideSqlOpenHelperCallback(): SupportSQLiteOpenHelper.Callback = mock()

    override fun provideSqlOpenHelper(context: Context, callback: SupportSQLiteOpenHelper.Callback): SupportSQLiteOpenHelper = mock()

    override val dateFactory: (Long?) -> Date = mock()

    override fun provideConfiguration(): CacheConfiguration<Glitch> = mock()

    override fun provideGsonSerialiser(): GsonSerialiser = mock()

    override fun provideStoreEntryFactory(gsonSerialiser: GsonSerialiser): StoreEntryFactory = mock()

    override fun provideEncryptionManager(storeEntryFactory: StoreEntryFactory): EncryptionManager? = mock()

    override fun provideSerialisationManager(encryptionManager: EncryptionManager?): SerialisationManager<Glitch> = mock()

    override fun provideDatabase(sqlOpenHelper: SupportSQLiteOpenHelper): SQLiteDatabase = mock()

    override fun provideDatabaseManager(database: SupportSQLiteDatabase, serialisationManager: SerialisationManager<Glitch>): DatabaseManager<Glitch> = mock()

    override fun mapToContentValues(map: Map<String, *>): ContentValues = mock()

    override fun provideCacheManager(databaseManager: DatabaseManager<Glitch>, emptyResponseFactory: EmptyResponseFactory<Glitch>): CacheManager<Glitch> = mock()

    override fun provideErrorInterceptorFactory(): CacheModule.Function3<CacheToken, Long, AnnotationProcessor.RxType, ErrorInterceptor<Glitch>> = mock()

    override fun provideCacheInterceptorFactory(cacheManager: CacheManager<Glitch>): CacheModule.Function2<CacheToken, Long, CacheInterceptor<Glitch>> = mock()

    override fun provideResponseInterceptor(metadataSubject: PublishSubject<CacheMetadata<Glitch>>, emptyResponseFactory: EmptyResponseFactory<Glitch>): CacheModule.Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<Glitch>> = mock()

    override fun provideDejaVuInterceptorFactory(errorInterceptorFactory: CacheModule.Function3<CacheToken, Long, AnnotationProcessor.RxType, ErrorInterceptor<Glitch>>, cacheInterceptorFactory: CacheModule.Function2<CacheToken, Long, CacheInterceptor<Glitch>>, responseInterceptor: CacheModule.Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<Glitch>>): DejaVuInterceptor.Factory<Glitch> = mock()

    override fun provideDefaultAdapterFactory(): RxJava2CallAdapterFactory = mock()

    override fun provideRetrofitCacheAdapterFactory(defaultAdapterFactory: RxJava2CallAdapterFactory, dejaVuInterceptorFactory: DejaVuInterceptor.Factory<Glitch>, processingErrorAdapterFactory: ProcessingErrorAdapter.Factory<Glitch>, annotationProcessor: AnnotationProcessor<Glitch>): RetrofitCallAdapterFactory<Glitch> = mock()

    override fun provideProcessingErrorAdapterFactory(errorInterceptorFactory: CacheModule.Function3<CacheToken, Long, AnnotationProcessor.RxType, ErrorInterceptor<Glitch>>, responseInterceptorFactory: CacheModule.Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<Glitch>>): ProcessingErrorAdapter.Factory<Glitch> = mock()

    override fun provideCacheMetadataSubject(): PublishSubject<CacheMetadata<Glitch>> = mock()

    override fun provideCacheMetadataObservable(subject: PublishSubject<CacheMetadata<Glitch>>): Observable<CacheMetadata<Glitch>> = mock()

    override fun provideAnnotationProcessor(): AnnotationProcessor<Glitch> = mock()

    override fun provideEmptyResponseFactory(): EmptyResponseFactory<Glitch> = mock()


}
