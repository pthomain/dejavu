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

package uk.co.glass_software.android.cache_interceptor.injection.module

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dagger.Module
import dagger.Provides
import io.reactivex.subjects.PublishSubject
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.cache_interceptor.configuration.CacheConfiguration
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.CacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.CacheManager
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database.DatabaseManager
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation.GsonSerialiser
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ErrorInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.response.EmptyResponseFactory
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.response.ResponseInterceptor
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.retrofit.ProcessingErrorAdapter
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory
import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.AnnotationProcessor
import uk.co.glass_software.android.shared_preferences.StoreEntryFactory
import uk.co.glass_software.android.shared_preferences.encryption.manager.EncryptionManager
import java.util.*
import uk.co.glass_software.android.cache_interceptor.injection.module.CacheModule.*
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database.SqlOpenHelperCallback
import javax.inject.Singleton

@Module
internal abstract class BaseCacheModule<E>(val configuration: CacheConfiguration<E>)
    : CacheModule<E>
        where E : Exception,
              E : NetworkErrorProvider {

    val DATABASE_NAME = "rx_cache_interceptor.db"
    val DATABASE_VERSION = 2

    @Provides
    @Singleton
    override fun provideContext() = configuration.context

    @Provides
    @Singleton
    override fun provideConfiguration() = configuration

    @Provides
    @Singleton
    override fun provideGsonSerialiser() =
            GsonSerialiser(configuration.gson)

    @Provides
    @Singleton
    override fun provideStoreEntryFactory(gsonSerialiser: GsonSerialiser) =
            StoreEntryFactory.builder(configuration.context)
                    .customSerialiser(gsonSerialiser)
                    .logger(configuration.logger)
                    .build()

    @Provides
    @Singleton
    override fun provideEncryptionManager(storeEntryFactory: StoreEntryFactory) =
            storeEntryFactory.encryptionManager

    @Provides
    @Singleton
    override fun provideSerialisationManager(encryptionManager: EncryptionManager?) =
            SerialisationManager<E>(
                    configuration.logger,
                    encryptionManager,
                    configuration.gson
            )

    override val dateFactory = { timeStamp: Long? -> timeStamp?.let { Date(it) } ?: Date() }

    @Provides
    @Singleton
    override fun provideSqlOpenHelperCallback(): SupportSQLiteOpenHelper.Callback =
            SqlOpenHelperCallback(
                    DATABASE_VERSION
            )

    @Provides
    @Singleton
    override fun provideDatabase(sqlOpenHelper: SupportSQLiteOpenHelper) =
            sqlOpenHelper.writableDatabase!!

    @Provides
    @Singleton
    override fun provideDatabaseManager(database: SupportSQLiteDatabase,
                                        serialisationManager: SerialisationManager<E>) =
            DatabaseManager(
                    database,
                    serialisationManager,
                    configuration.logger,
                    configuration.compress,
                    configuration.encrypt,
                    configuration.cacheDurationInMillis,
                    dateFactory,
                    this::mapToContentValues
            )

    @Provides
    @Singleton
    override fun provideCacheManager(databaseManager: DatabaseManager<E>,
                                     emptyResponseFactory: EmptyResponseFactory<E>) =
            CacheManager(
                    databaseManager,
                    emptyResponseFactory,
                    dateFactory,
                    configuration.cacheDurationInMillis,
                    configuration.logger
            )

    @Provides
    @Singleton
    override fun provideErrorInterceptorFactory(): Function3<CacheToken, Long, AnnotationProcessor.RxType, ErrorInterceptor<E>> =
            object : Function3<CacheToken, Long, AnnotationProcessor.RxType, ErrorInterceptor<E>> {
                override fun get(t1: CacheToken, t2: Long, t3: AnnotationProcessor.RxType) = ErrorInterceptor(
                        configuration.errorFactory,
                        configuration.logger,
                        t1,
                        t2,
                        t3,
                        configuration.requestTimeOutInSeconds
                )
            }

    @Provides
    @Singleton
    override fun provideCacheInterceptorFactory(cacheManager: CacheManager<E>): Function2<CacheToken, Long, CacheInterceptor<E>> =
            object : Function2<CacheToken, Long, CacheInterceptor<E>> {
                override fun get(t1: CacheToken, t2: Long) = CacheInterceptor(
                        cacheManager,
                        configuration.isCacheEnabled,
                        configuration.logger,
                        t1,
                        t2
                )
            }

    @Provides
    @Singleton
    override fun provideResponseInterceptor(metadataSubject: PublishSubject<CacheMetadata<E>>,
                                            emptyResponseFactory: EmptyResponseFactory<E>): Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>> =
            object : Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>> {
                override fun get(t1: CacheToken,
                                 t2: Boolean,
                                 t3: Boolean,
                                 t4: Long) = ResponseInterceptor(
                        configuration.logger,
                        emptyResponseFactory,
                        configuration,
                        metadataSubject,
                        t1,
                        t2,
                        t3,
                        t4,
                        configuration.mergeOnNextOnError
                )
            }

    @Provides
    @Singleton
    override fun provideRxCacheInterceptorFactory(errorInterceptorFactory: Function3<CacheToken, Long, AnnotationProcessor.RxType, ErrorInterceptor<E>>,
                                                  cacheInterceptorFactory: Function2<CacheToken, Long, CacheInterceptor<E>>,
                                                  responseInterceptor: Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>>) =
            RxCacheInterceptor.Factory(
                    { token, start, rxType -> errorInterceptorFactory.get(token, start, rxType) },
                    { token, start -> cacheInterceptorFactory.get(token, start) },
                    { token, isSingle, isCompletable, start -> responseInterceptor.get(token, isSingle, isCompletable, start) },
                    configuration
            )

    @Provides
    @Singleton
    override fun provideDefaultAdapterFactory() =
            RxJava2CallAdapterFactory.create()!!

    @Provides
    @Singleton
    override fun provideRetrofitCacheAdapterFactory(defaultAdapterFactory: RxJava2CallAdapterFactory,
                                                    rxCacheInterceptorFactory: RxCacheInterceptor.Factory<E>,
                                                    processingErrorAdapterFactory: ProcessingErrorAdapter.Factory<E>,
                                                    annotationProcessor: AnnotationProcessor<E>) =
            RetrofitCacheAdapterFactory(
                    defaultAdapterFactory,
                    rxCacheInterceptorFactory,
                    annotationProcessor,
                    processingErrorAdapterFactory,
                    configuration.logger
            )

    @Provides
    @Singleton
    override fun provideProcessingErrorAdapterFactory(errorInterceptorFactory: Function3<CacheToken, Long, AnnotationProcessor.RxType, ErrorInterceptor<E>>,
                                                      responseInterceptorFactory: Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>>) =
            ProcessingErrorAdapter.Factory(
                    { token, start, rxType -> errorInterceptorFactory.get(token, start, rxType) },
                    { token, isSingle, isCompletable, start -> responseInterceptorFactory.get(token, isSingle, isCompletable, start) }
            )

    @Provides
    @Singleton
    override fun provideCacheMetadataSubject() =
            PublishSubject.create<CacheMetadata<E>>()

    @Provides
    @Singleton
    override fun provideCacheMetadataObservable(subject: PublishSubject<CacheMetadata<E>>) =
            subject.map { it }!!

    @Provides
    @Singleton
    override fun provideAnnotationProcessor() =
            AnnotationProcessor(configuration)

    @Provides
    @Singleton
    override fun provideEmptyResponseFactory() =
            EmptyResponseFactory(configuration.errorFactory)

}
