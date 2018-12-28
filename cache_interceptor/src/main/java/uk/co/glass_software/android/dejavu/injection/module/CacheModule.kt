package uk.co.glass_software.android.dejavu.injection.module

import android.content.ContentValues
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import retrofit2.CallAdapter
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.dejavu.configuration.CacheConfiguration
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.DejaVuInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.CacheInterceptor
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.CacheManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.database.DatabaseManager
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation.GsonSerialiser
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
import java.util.*

internal interface CacheModule<E>
        where E : Exception,
              E : NetworkErrorProvider {

    fun provideContext(): Context

    fun provideConfiguration(): CacheConfiguration<E>

    fun provideGsonSerialiser(): GsonSerialiser

    fun provideStoreEntryFactory(gsonSerialiser: GsonSerialiser): StoreEntryFactory

    fun provideEncryptionManager(storeEntryFactory: StoreEntryFactory): EncryptionManager?

    fun provideDateFactory(): Function1<Long?, Date>

    fun provideCompresser(): Function1<ByteArray, ByteArray>

    fun provideUncompresser(): Function3<ByteArray, Int, Int, ByteArray>

    fun provideByteToStringConverter(): Function1<ByteArray, String>

    fun provideSerialisationManager(encryptionManager: EncryptionManager?,
                                    byteToStringConverter: Function1<ByteArray, String>,
                                    compresser: Function1<ByteArray, ByteArray>,
                                    uncompresser: Function3<ByteArray, Int, Int, ByteArray>): SerialisationManager<E>

    fun provideSqlOpenHelperCallback(): SupportSQLiteOpenHelper.Callback

    fun provideSqlOpenHelper(context: Context,
                             callback: SupportSQLiteOpenHelper.Callback): SupportSQLiteOpenHelper

    fun provideDatabase(sqlOpenHelper: SupportSQLiteOpenHelper): SupportSQLiteDatabase

    fun provideHasher(): Hasher

    fun provideDatabaseManager(database: SupportSQLiteDatabase,
                               hasher: Hasher,
                               dateFactory: Function1<Long?, Date>,
                               serialisationManager: SerialisationManager<E>): DatabaseManager<E>

    fun provideCacheManager(databaseManager: DatabaseManager<E>,
                            dateFactory: Function1<Long?, Date>,
                            emptyResponseFactory: EmptyResponseFactory<E>): CacheManager<E>

    fun provideErrorInterceptorFactory(dateFactory: Function1<Long?, Date>): Function2<CacheToken, Long, ErrorInterceptor<E>>

    fun provideCacheInterceptorFactory(cacheManager: CacheManager<E>): Function2<CacheToken, Long, CacheInterceptor<E>>

    fun provideResponseInterceptor(dateFactory: Function1<Long?, Date>,
                                   metadataSubject: PublishSubject<CacheMetadata<E>>,
                                   emptyResponseFactory: EmptyResponseFactory<E>): Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>>

    fun provideDejaVuInterceptorFactory(dateFactory: Function1<Long?, Date>,
                                        errorInterceptorFactory: Function2<CacheToken, Long, ErrorInterceptor<E>>,
                                        cacheInterceptorFactory: Function2<CacheToken, Long, CacheInterceptor<E>>,
                                        responseInterceptor: Function4<CacheToken, Boolean, Boolean, Long, ResponseInterceptor<E>>): DejaVuInterceptor.Factory<E>

    fun provideDefaultAdapterFactory(): RxJava2CallAdapterFactory

    fun provideRetrofitCallAdapterInnerFactory(): Function5<DejaVuInterceptor.Factory<E>, Logger, String, CacheInstruction?, CallAdapter<Any, Any>, RetrofitCallAdapter<E>>

    fun provideRetrofitCallAdapterFactory(dateFactory: Function1<Long?, Date>,
                                          innerFactory: Function5<DejaVuInterceptor.Factory<E>, Logger, String, CacheInstruction?, CallAdapter<Any, Any>, RetrofitCallAdapter<E>>,
                                          defaultAdapterFactory: RxJava2CallAdapterFactory,
                                          dejaVuInterceptorFactory: DejaVuInterceptor.Factory<E>,
                                          processingErrorAdapterFactory: ProcessingErrorAdapter.Factory<E>,
                                          annotationProcessor: AnnotationProcessor<E>): RetrofitCallAdapterFactory<E>

    fun provideProcessingErrorAdapterFactory(errorInterceptorFactory: Function2<CacheToken, Long, ErrorInterceptor<E>>,
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

    interface Function5<T1, T2, T3, T4, T5, R> {
        fun get(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5): R
    }
}