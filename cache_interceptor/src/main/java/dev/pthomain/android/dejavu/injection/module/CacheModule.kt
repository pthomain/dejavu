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

package dev.pthomain.android.dejavu.injection.module

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.configuration.CacheInstruction
import dev.pthomain.android.dejavu.configuration.NetworkErrorProvider
import dev.pthomain.android.dejavu.configuration.Serialiser
import dev.pthomain.android.dejavu.interceptors.DejaVuInterceptor
import dev.pthomain.android.dejavu.interceptors.internal.cache.CacheInterceptor
import dev.pthomain.android.dejavu.interceptors.internal.cache.CacheManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.CacheMetadataManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.PersistenceManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.database.DatabasePersistenceManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.file.FileNameSerialiser
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.file.FilePersistenceManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.statistics.StatisticsCompiler
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.statistics.database.DatabaseStatisticsCompiler
import dev.pthomain.android.dejavu.interceptors.internal.cache.persistence.statistics.file.FileStatisticsCompiler
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.Hasher
import dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation.SerialisationManager
import dev.pthomain.android.dejavu.interceptors.internal.cache.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.internal.error.ErrorInterceptor
import dev.pthomain.android.dejavu.interceptors.internal.response.EmptyResponseFactory
import dev.pthomain.android.dejavu.interceptors.internal.response.ResponseInterceptor
import dev.pthomain.android.dejavu.response.CacheMetadata
import dev.pthomain.android.dejavu.retrofit.ProcessingErrorAdapter
import dev.pthomain.android.dejavu.retrofit.RetrofitCallAdapter
import dev.pthomain.android.dejavu.retrofit.RetrofitCallAdapterFactory
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor
import dev.pthomain.android.mumbo.base.EncryptionManager
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import retrofit2.CallAdapter
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.util.*

internal interface CacheModule<E>
        where E : Exception,
              E : NetworkErrorProvider {

    fun provideContext(): Context

    fun provideConfiguration(): CacheConfiguration<E>

    fun provideSerialiser(): Serialiser

    fun provideEncryptionManager(): EncryptionManager

    fun provideFileNameSerialiser(): FileNameSerialiser

    fun provideFilePersistenceManagerFactory(hasher: Hasher,
                                             serialisationManager: SerialisationManager<E>,
                                             dateFactory: Function1<Long?, Date>,
                                             fileNameSerialiser: FileNameSerialiser): FilePersistenceManager.Factory<E>

    fun provideDateFactory(): Function1<Long?, Date>

    fun provideCompresser(): Function1<ByteArray, ByteArray>

    fun provideUncompresser(): Function3<ByteArray, Int, Int, ByteArray>

    fun provideByteToStringConverter(): Function1<ByteArray, String>

    fun provideSerialisationManager(encryptionManager: EncryptionManager,
                                    byteToStringConverter: Function1<ByteArray, String>,
                                    compresser: Function1<ByteArray, ByteArray>,
                                    uncompresser: Function3<ByteArray, Int, Int, ByteArray>): SerialisationManager<E>

    fun provideSqlOpenHelperCallback(): SupportSQLiteOpenHelper.Callback?

    fun provideSqlOpenHelper(context: Context,
                             callback: SupportSQLiteOpenHelper.Callback?): SupportSQLiteOpenHelper?

    fun provideDatabase(sqlOpenHelper: SupportSQLiteOpenHelper?): SupportSQLiteDatabase?

    fun provideHasher(uriParser: Function1<String, Uri>): Hasher

    fun providePersistenceManager(databasePersistenceManager: DatabasePersistenceManager<E>?,
                                  filePersistenceManagerFactory: FilePersistenceManager.Factory<E>): PersistenceManager<E>

    fun provideDatabasePersistenceManager(hasher: Hasher,
                                          database: SupportSQLiteDatabase?,
                                          dateFactory: Function1<Long?, Date>,
                                          serialisationManager: SerialisationManager<E>): DatabasePersistenceManager<E>?

    fun provideDatabaseStatisticsCompiler(database: SupportSQLiteDatabase?,
                                          dateFactory: Function1<Long?, Date>): DatabaseStatisticsCompiler?

    fun provideFileStatisticsCompiler(fileNameSerialiser: FileNameSerialiser,
                                      dateFactory: Function1<Long?, Date>): FileStatisticsCompiler

    fun provideStatisticsCompiler(fileStatisticsCompiler: FileStatisticsCompiler,
                                  databaseStatisticsCompiler: DatabaseStatisticsCompiler?): StatisticsCompiler?

    fun provideCacheMetadataManager(persistenceManager: PersistenceManager<E>,
                                    dateFactory: Function1<Long?, Date>,
                                    emptyResponseFactory: EmptyResponseFactory<E>): CacheMetadataManager<E>

    fun provideCacheManager(persistenceManager: PersistenceManager<E>,
                            cacheMetadataManager: CacheMetadataManager<E>,
                            dateFactory: Function1<Long?, Date>,
                            emptyResponseFactory: EmptyResponseFactory<E>): CacheManager<E>

    fun provideErrorInterceptorFactory(dateFactory: Function1<Long?, Date>): Function3<Context, CacheToken, Long, ErrorInterceptor<E>>

    fun provideCacheInterceptorFactory(dateFactory: Function1<Long?, Date>,
                                       cacheManager: CacheManager<E>): Function2<CacheToken, Long, CacheInterceptor<E>>

    fun provideResponseInterceptor(dateFactory: Function1<Long?, Date>,
                                   metadataSubject: PublishSubject<CacheMetadata<E>>,
                                   emptyResponseFactory: EmptyResponseFactory<E>): Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>>

    fun provideDejaVuInterceptorFactory(hasher: Hasher,
                                        dateFactory: Function1<Long?, Date>,
                                        errorInterceptorFactory: Function3<Context, CacheToken, Long, ErrorInterceptor<E>>,
                                        cacheInterceptorFactory: Function2<CacheToken, Long, CacheInterceptor<E>>,
                                        responseInterceptor: Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>>): DejaVuInterceptor.Factory<E>

    fun provideDefaultAdapterFactory(): RxJava2CallAdapterFactory

    fun provideUriParser(): Function1<String, Uri>

    fun provideRetrofitCallAdapterInnerFactory(): Function6<DejaVuInterceptor.Factory<E>, Logger, String, Class<*>, CacheInstruction?, CallAdapter<Any, Any>, RetrofitCallAdapter<E>>

    fun provideRetrofitCallAdapterFactory(dateFactory: Function1<Long?, Date>,
                                          innerFactory: Function6<DejaVuInterceptor.Factory<E>, Logger, String, Class<*>, CacheInstruction?, CallAdapter<Any, Any>, RetrofitCallAdapter<E>>,
                                          defaultAdapterFactory: RxJava2CallAdapterFactory,
                                          dejaVuInterceptorFactory: DejaVuInterceptor.Factory<E>,
                                          processingErrorAdapterFactory: ProcessingErrorAdapter.Factory<E>,
                                          annotationProcessor: AnnotationProcessor<E>): RetrofitCallAdapterFactory<E>

    fun provideProcessingErrorAdapterFactory(errorInterceptorFactory: Function3<Context, CacheToken, Long, ErrorInterceptor<E>>,
                                             dateFactory: Function1<Long?, Date>,
                                             responseInterceptorFactory: Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>>): ProcessingErrorAdapter.Factory<E>

    fun provideCacheMetadataSubject(): PublishSubject<CacheMetadata<E>>

    fun provideCacheMetadataObservable(subject: PublishSubject<CacheMetadata<E>>): Observable<CacheMetadata<E>>

    fun provideAnnotationProcessor(): AnnotationProcessor<E>

    fun provideEmptyResponseFactory(): EmptyResponseFactory<E>

    fun mapToContentValues(map: Map<String, *>): ContentValues {
        val values = ContentValues()
        for ((key, value) in map) {
            when (value) {
                is Boolean -> values.put(key, value)
                is Float -> values.put(key, value)
                is Double -> values.put(key, value)
                is Long -> values.put(key, value)
                is Int -> values.put(key, value)
                is Byte -> values.put(key, value)
                is ByteArray -> values.put(key, value)
                is Short -> values.put(key, value)
                is String -> values.put(key, value)
            }
        }
        return values
    }

    interface Function1<T1, R> {
        fun get(t1: T1): R
    }

    interface Function2<T1, T2, R> {
        fun get(t1: T1, t2: T2): R
    }

    interface Function3<T1, T2, T3, R> {
        fun get(t1: T1, t2: T2, t3: T3): R
    }

    interface Function4<T1, T2, T3, T4, R> {
        fun get(t1: T1, t2: T2, t3: T3, t4: T4): R
    }

    interface Function6<T1, T2, T3, T4, T5, T6, R> {
        fun get(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6): R
    }
}