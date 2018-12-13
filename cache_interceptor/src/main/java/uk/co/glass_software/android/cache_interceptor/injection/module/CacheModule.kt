package uk.co.glass_software.android.cache_interceptor.injection.module

import android.content.ContentValues
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import io.reactivex.Observable
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

internal interface CacheModule<E>
        where E : Exception,
              E : NetworkErrorProvider {

    val dateFactory: (Long?) -> Date

    fun provideContext(): Context

    fun provideConfiguration(): CacheConfiguration<E>

    fun provideGsonSerialiser(): GsonSerialiser

    fun provideStoreEntryFactory(gsonSerialiser: GsonSerialiser): StoreEntryFactory

    fun provideEncryptionManager(storeEntryFactory: StoreEntryFactory): EncryptionManager?

    fun provideSerialisationManager(encryptionManager: EncryptionManager?): SerialisationManager<E>

    fun provideSqlOpenHelperCallback(): SupportSQLiteOpenHelper.Callback

    fun provideSqlOpenHelper(context: Context,
                             callback: SupportSQLiteOpenHelper.Callback): SupportSQLiteOpenHelper

    fun provideDatabase(sqlOpenHelper: SupportSQLiteOpenHelper): SupportSQLiteDatabase

    fun provideDatabaseManager(database: SupportSQLiteDatabase,
                               serialisationManager: SerialisationManager<E>): DatabaseManager<E>

    fun provideCacheManager(databaseManager: DatabaseManager<E>,
                            emptyResponseFactory: EmptyResponseFactory<E>): CacheManager<E>

    fun provideErrorInterceptorFactory(): Function3<CacheToken, Long, AnnotationProcessor.RxType, ErrorInterceptor<E>>

    fun provideCacheInterceptorFactory(cacheManager: CacheManager<E>): Function2<CacheToken, Long, CacheInterceptor<E>>

    fun provideResponseInterceptor(metadataSubject: PublishSubject<CacheMetadata<E>>,
                                   emptyResponseFactory: EmptyResponseFactory<E>): Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>>

    fun provideRxCacheInterceptorFactory(errorInterceptorFactory: Function3<CacheToken, Long, AnnotationProcessor.RxType, ErrorInterceptor<E>>,
                                         cacheInterceptorFactory: Function2<CacheToken, Long, CacheInterceptor<E>>,
                                         responseInterceptor: Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>>): RxCacheInterceptor.Factory<E>

    fun provideDefaultAdapterFactory(): RxJava2CallAdapterFactory

    fun provideRetrofitCacheAdapterFactory(defaultAdapterFactory: RxJava2CallAdapterFactory,
                                           rxCacheInterceptorFactory: RxCacheInterceptor.Factory<E>,
                                           processingErrorAdapterFactory: ProcessingErrorAdapter.Factory<E>,
                                           annotationProcessor: AnnotationProcessor<E>): RetrofitCacheAdapterFactory<E>

    fun provideProcessingErrorAdapterFactory(errorInterceptorFactory: Function3<CacheToken, Long, AnnotationProcessor.RxType, ErrorInterceptor<E>>,
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

    interface Function2<T1, T2, R> {
        fun get(t1: T1, t2: T2): R
    }

    interface Function3<T1, T2, T3, R> {
        fun get(t1: T1, t2: T2, t3: T3): R
    }

    interface Function4<T1, T2, T3, T4, R> {
        fun get(t1: T1, t2: T2, t3: T3, t4: T4): R
    }
}