package uk.co.glass_software.android.cache_interceptor.interceptors.cache

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.support.annotation.RestrictTo
import android.support.annotation.VisibleForTesting

import com.google.gson.Gson

import java.util.Date

import uk.co.glass_software.android.shared_preferences.StoreEntryFactory
import uk.co.glass_software.android.shared_preferences.utils.Logger

class CacheInterceptorBuilder<E> internal constructor()
        where E : Exception,
              E : (E) -> Boolean {

    private var databaseName: String? = null
    private var logger: Logger? = null
    private var gson: Gson? = null

    fun gson(gson: Gson): CacheInterceptorBuilder<E> {
        this.gson = gson
        return this
    }

    fun databaseName(databaseName: String): CacheInterceptorBuilder<E> {
        this.databaseName = databaseName
        return this
    }

    fun logger(logger: Logger): CacheInterceptorBuilder<E> {
        this.logger = logger
        return this
    }

    //Used for unit testing
    @VisibleForTesting
    internal fun mapToContentValues(map: Map<String, *>): ContentValues {
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

    @SuppressLint("RestrictedApi")
    @JvmOverloads
    fun build(context: Context,
              compressData: Boolean = false,
              encryptData: Boolean = false) = build(
            context,
            compressData,
            encryptData,
            null
    )

    @RestrictTo(RestrictTo.Scope.TESTS)
    internal fun build(context: Context,
                       compressData: Boolean,
                       encryptData: Boolean,
                       holder: Holder<E>?): CacheInterceptor.Factory<E> {
        val gson = gson ?: Gson()

        val logger = this.logger ?: object : Logger {
            override fun e(caller: Any, t: Throwable, message: String) {}
            override fun e(caller: Any, message: String) {}
            override fun d(caller: Any, message: String) {}
        }

        val storeEntryFactory = StoreEntryFactory.builder(context)
                .customSerialiser(GsonSerialiser(gson))
                .logger(logger)
                .build()

        val serialisationManager = SerialisationManager<E>(
                logger,
                storeEntryFactory,
                encryptData,
                compressData,
                gson
        )

        val database = SqlOpenHelper(
                context.applicationContext,
                databaseName ?: DATABASE_NAME
        ).writableDatabase

        val dateFactory: (Long?) -> Date = { it?.let { Date(it) } ?: Date() }

        val databaseManager = DatabaseManager(
                database,
                serialisationManager,
                logger,
                dateFactory,
                this::mapToContentValues
        )

        val cacheManager = CacheManager<E>(
                databaseManager,
                dateFactory,
                gson,
                logger
        )

        if (holder != null) {
            holder.gson = gson
            holder.serialisationManager = serialisationManager
            holder.database = database
            holder.dateFactory = dateFactory
            holder.databaseManager = databaseManager
            holder.cacheManager = cacheManager
        }

        return CacheInterceptor.Factory(
                cacheManager,
                true,
                logger
        )
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    internal class Holder<E>
            where E : Exception,
                  E : (E) -> Boolean {
        var gson: Gson? = null
        var serialisationManager: SerialisationManager<E>? = null
        var database: SQLiteDatabase? = null
        var dateFactory: ((Long?) -> Date)? = null
        var databaseManager: DatabaseManager<E>? = null
        var cacheManager: CacheManager<E>? = null
    }

    companion object {
        const val DATABASE_NAME = "rx_cache_interceptor.db"
    }
}
