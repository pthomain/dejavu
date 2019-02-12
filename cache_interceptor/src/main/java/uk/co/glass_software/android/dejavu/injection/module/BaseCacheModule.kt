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

package uk.co.glass_software.android.dejavu.injection.module

import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dagger.Module
import dagger.Provides
import io.reactivex.subjects.PublishSubject
import org.iq80.snappy.Snappy
import retrofit2.CallAdapter
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstructionSerialiser
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.injection.module.CacheModule.*
import uk.co.glass_software.android.dejavu.interceptors.DejaVuInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.CacheInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.CacheManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.DatabaseManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.SqlOpenHelperCallback
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.Hasher
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.ErrorInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.response.EmptyResponseFactory
import uk.co.glass_software.android.dejavu.interceptors.internal.response.ResponseInterceptor
import uk.co.glass_software.android.dejavu.response.CacheMetadata
import uk.co.glass_software.android.dejavu.retrofit.ProcessingErrorAdapter
import uk.co.glass_software.android.dejavu.retrofit.RetrofitCallAdapter
import uk.co.glass_software.android.dejavu.retrofit.RetrofitCallAdapterFactory
import uk.co.glass_software.android.dejavu.retrofit.annotations.AnnotationProcessor
import uk.co.glass_software.android.shared_preferences.StoreEntryFactory
import uk.co.glass_software.android.shared_preferences.encryption.manager.EncryptionManager
import uk.co.glass_software.android.shared_preferences.persistence.serialisation.Serialiser
import java.util.*
import javax.inject.Singleton

@Module
internal abstract class BaseCacheModule<E>(val configuration: CacheConfiguration<E>)
    : CacheModule<E>
        where E : Exception,
              E : NetworkErrorProvider {

    val DATABASE_NAME = "dejavu.db"
    val DATABASE_VERSION = 1

    @Provides
    @Singleton
    override fun provideContext() = configuration.context

    @Provides
    @Singleton
    override fun provideConfiguration() = configuration

    @Provides
    @Singleton
    override fun provideSerialiser() = configuration.serialiser

    @Provides
    @Singleton
    override fun provideStoreEntryFactory(serialiser: Serialiser) =
            StoreEntryFactory.builder(configuration.context)
                    .customSerialiser(serialiser)
                    .logger(configuration.logger)
                    .build()

    @Provides
    @Singleton
    override fun provideEncryptionManager(storeEntryFactory: StoreEntryFactory) =
            storeEntryFactory.encryptionManager

    @Provides
    @Singleton
    override fun provideCompresser() = object : Function1<ByteArray, ByteArray> {
        override fun get(t1: ByteArray) =
                Snappy.compress(t1)
    }

    @Provides
    @Singleton
    override fun provideUncompresser() = object : Function3<ByteArray, Int, Int, ByteArray> {
        override fun get(t1: ByteArray, t2: Int, t3: Int) =
                Snappy.uncompress(t1, t2, t3)
    }

    @Provides
    @Singleton
    override fun provideByteToStringConverter() = object : Function1<ByteArray, String> {
        override fun get(t1: ByteArray) = String(t1)
    }

    @Provides
    @Singleton
    override fun provideSerialisationManager(encryptionManager: EncryptionManager?,
                                             byteToStringConverter: Function1<ByteArray, String>,
                                             compresser: Function1<ByteArray, ByteArray>,
                                             uncompresser: Function3<ByteArray, Int, Int, ByteArray>) =
            SerialisationManager<E>(
                    configuration.logger,
                    { byteToStringConverter.get(it) },
                    encryptionManager,
                    { compresser.get(it) },
                    { compressed, compressedOffset, compressedSize -> uncompresser.get(compressed, compressedOffset, compressedSize) },
                    configuration.serialiser
            )

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
    override fun provideHasher(uriParser: Function1<String, Uri>) =
            Hasher.Factory(
                    configuration.logger,
                    uriParser::get
            ).create()

    @Provides
    @Singleton
    override fun provideDatabaseManager(database: SupportSQLiteDatabase,
                                        dateFactory: Function1<Long?, Date>,
                                        serialisationManager: SerialisationManager<E>) =
            DatabaseManager(
                    database,
                    serialisationManager,
                    configuration.logger,
                    configuration.compress,
                    configuration.encrypt,
                    configuration.cacheDurationInMillis,
                    { dateFactory.get(it) },
                    this::mapToContentValues
            )

    @Provides
    @Singleton
    override fun provideCacheManager(serialiser: Serialiser,
                                     databaseManager: DatabaseManager<E>,
                                     dateFactory: Function1<Long?, Date>,
                                     emptyResponseFactory: EmptyResponseFactory<E>) =
            CacheManager(
                    configuration.errorFactory,
                    serialiser,
                    databaseManager,
                    emptyResponseFactory,
                    { dateFactory.get(it) },
                    configuration.cacheDurationInMillis,
                    configuration.logger
            )

    @Provides
    @Singleton
    override fun provideErrorInterceptorFactory(dateFactory: Function1<Long?, Date>): Function2<CacheToken, Long, ErrorInterceptor<E>> =
            object : Function2<CacheToken, Long, ErrorInterceptor<E>> {
                override fun get(t1: CacheToken, t2: Long) = ErrorInterceptor(
                        configuration.errorFactory,
                        configuration.logger,
                        { dateFactory.get(it) },
                        t1,
                        t2,
                        configuration.requestTimeOutInSeconds
                )
            }

    @Provides
    @Singleton
    override fun provideCacheInterceptorFactory(dateFactory: Function1<Long?, Date>,
                                                cacheManager: CacheManager<E>): Function2<CacheToken, Long, CacheInterceptor<E>> =
            object : Function2<CacheToken, Long, CacheInterceptor<E>> {
                override fun get(t1: CacheToken, t2: Long) = CacheInterceptor(
                        cacheManager,
                        { dateFactory.get(it) },
                        configuration.isCacheEnabled,
                        configuration.logger,
                        t1,
                        t2
                )
            }

    @Provides
    @Singleton
    override fun provideResponseInterceptor(dateFactory: Function1<Long?, Date>,
                                            metadataSubject: PublishSubject<CacheMetadata<E>>,
                                            emptyResponseFactory: EmptyResponseFactory<E>): Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>> =
            object : Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>> {
                override fun get(t1: CacheToken,
                                 t2: Boolean,
                                 t3: Boolean,
                                 t4: Long) = ResponseInterceptor(
                        configuration.logger,
                        { dateFactory.get(it) },
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
    override fun provideDejaVuInterceptorFactory(hasher: Hasher,
                                                 dateFactory: Function1<Long?, Date>,
                                                 errorInterceptorFactory: Function2<CacheToken, Long, ErrorInterceptor<E>>,
                                                 cacheInterceptorFactory: Function2<CacheToken, Long, CacheInterceptor<E>>,
                                                 responseInterceptor: Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>>) =
            DejaVuInterceptor.Factory(
                    hasher,
                    { dateFactory.get(it) },
                    { token, start -> errorInterceptorFactory.get(token, start) },
                    { token, start -> cacheInterceptorFactory.get(token, start) },
                    { token, isSingle, isCompletable, start -> responseInterceptor.get(token, isSingle, isCompletable, start) },
                    configuration
            )

    @Provides
    @Singleton
    override fun provideDefaultAdapterFactory() = RxJava2CallAdapterFactory.create()!!

    @Provides
    @Singleton
    override fun provideUriParser() =
            object : Function1<String, Uri> {
                override fun get(t1: String) = Uri.parse(t1)
            }

    @Provides
    @Singleton
    override fun provideRetrofitCallAdapterInnerFactory() =
            object : Function5<DejaVuInterceptor.Factory<E>, Logger, String, CacheInstruction?, CallAdapter<Any, Any>, RetrofitCallAdapter<E>> {
                override fun get(
                        t1: DejaVuInterceptor.Factory<E>,
                        t2: Logger,
                        t3: String,
                        t4: CacheInstruction?,
                        t5: CallAdapter<Any, Any>
                ) = RetrofitCallAdapter(
                        t1,
                        CacheInstructionSerialiser(),
                        t2,
                        t3,
                        t4,
                        t5
                )
            }

    @Provides
    @Singleton
    override fun provideRetrofitCallAdapterFactory(dateFactory: Function1<Long?, Date>,
                                                   innerFactory: Function5<DejaVuInterceptor.Factory<E>, Logger, String, CacheInstruction?, CallAdapter<Any, Any>, RetrofitCallAdapter<E>>,
                                                   defaultAdapterFactory: RxJava2CallAdapterFactory,
                                                   dejaVuInterceptorFactory: DejaVuInterceptor.Factory<E>,
                                                   processingErrorAdapterFactory: ProcessingErrorAdapter.Factory<E>,
                                                   annotationProcessor: AnnotationProcessor<E>) =
            RetrofitCallAdapterFactory(
                    defaultAdapterFactory,
                    { t1, t2, t3, t4, t5 -> innerFactory.get(t1, t2, t3, t4, t5) },
                    { dateFactory.get(it) },
                    dejaVuInterceptorFactory,
                    annotationProcessor,
                    processingErrorAdapterFactory,
                    configuration.logger
            )

    @Provides
    @Singleton
    override fun provideProcessingErrorAdapterFactory(errorInterceptorFactory: Function2<CacheToken, Long, ErrorInterceptor<E>>,
                                                      dateFactory: Function1<Long?, Date>,
                                                      responseInterceptorFactory: Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>>) =
            ProcessingErrorAdapter.Factory(
                    { token, start -> errorInterceptorFactory.get(token, start) },
                    { token, isSingle, isCompletable, start -> responseInterceptorFactory.get(token, isSingle, isCompletable, start) },
                    { dateFactory.get(it) }
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
