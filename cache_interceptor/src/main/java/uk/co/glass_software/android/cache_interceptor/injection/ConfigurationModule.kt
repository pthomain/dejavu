package uk.co.glass_software.android.cache_interceptor.injection

import android.content.ContentValues
import dagger.Module
import dagger.Provides
import io.requery.android.database.sqlite.SQLiteDatabase
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.boilerplate.Boilerplate.context
import uk.co.glass_software.android.cache_interceptor.annotations.AnnotationHelper
import uk.co.glass_software.android.cache_interceptor.configuration.CacheConfiguration
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.ResponseInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.CacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.CacheManager
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database.DatabaseManager
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.database.SqlOpenHelper
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation.GsonSerialiser
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation.SerialisationManager
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ErrorInterceptor
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory
import uk.co.glass_software.android.shared_preferences.StoreEntryFactory
import uk.co.glass_software.android.shared_preferences.encryption.manager.EncryptionManager
import java.util.*

@Module
internal abstract class ConfigurationModule<E>(private val configuration: CacheConfiguration<E>)
        where E : Exception,
              E : NetworkErrorProvider {

    @Provides
    fun provideGsonSerialiser() =
            GsonSerialiser(configuration.gson)

    @Provides
    fun provideStoreEntryFactory(gsonSerialiser: GsonSerialiser) =
            StoreEntryFactory.builder(context)
                    .customSerialiser(gsonSerialiser)
                    .logger(configuration.logger)
                    .build()

    @Provides
    fun provideEncryptionManager(storeEntryFactory: StoreEntryFactory) =
            storeEntryFactory.encryptionManager

    @Provides
    fun provideSerialisationManager(encryptionManager: EncryptionManager?) =
            SerialisationManager<E>(
                    configuration.logger,
                    encryptionManager,
                    configuration.gson
            )

    @Provides
    fun provideDatabase() = SqlOpenHelper(
            context.applicationContext,
            configuration.databaseName
    ).writableDatabase!!

    private val dateFactory = { timeStamp: Long? -> timeStamp?.let { Date(it) } ?: Date() }

    @Provides
    fun provideDatabaseManager(database: SQLiteDatabase,
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


    private fun mapToContentValues(map: Map<String, *>): ContentValues {
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

    @Provides
    fun cacheManager(databaseManager: DatabaseManager<E>) =
            CacheManager(
                    databaseManager,
                    dateFactory,
                    configuration.cacheDurationInMillis,
                    configuration.logger
            )

    @Provides
    fun provideErrorInterceptorFactory() = object : Function<CacheToken, ErrorInterceptor<E>> {
        override fun get(t: CacheToken) = ErrorInterceptor(
                configuration.errorFactory,
                configuration.logger,
                t,
                configuration.networkTimeOutInSeconds
        )
    }

    @Provides
    fun provideCacheInterceptorFactory(cacheManager: CacheManager<E>) = object : Function<CacheToken, CacheInterceptor<E>> {
        override fun get(t: CacheToken) = CacheInterceptor(
                cacheManager,
                configuration.isCacheEnabled,
                configuration.logger,
                t
        )
    }

    @Provides
    fun provideResponseInterceptor() = ResponseInterceptor<E>(
            configuration.logger,
            configuration.mergeOnNextOnError
    )

    @Provides
    fun provideRxCacheInterceptorFactory(errorInterceptorFactory: Function<CacheToken, ErrorInterceptor<E>>,
                                         cacheInterceptorFactory: Function<CacheToken, CacheInterceptor<E>>,
                                         responseInterceptor: ResponseInterceptor<E>) =
            RxCacheInterceptor.Factory(
                    { errorInterceptorFactory.get(it) },
                    { cacheInterceptorFactory.get(it) },
                    responseInterceptor,
                    configuration
            )

    @Provides
    fun provideRetrofitCacheAdapterFactory(rxCacheInterceptorFactory: RxCacheInterceptor.Factory<E>) =
            RetrofitCacheAdapterFactory(
                    RxJava2CallAdapterFactory.create(),
                    rxCacheInterceptorFactory,
                    AnnotationHelper()
            )

    interface Function<T, R> {
        fun get(t: T): R
    }
}
