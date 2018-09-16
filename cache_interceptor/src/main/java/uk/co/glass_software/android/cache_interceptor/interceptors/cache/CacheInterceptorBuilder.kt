package uk.co.glass_software.android.cache_interceptor.interceptors.cache

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.support.annotation.RestrictTo
import android.support.annotation.VisibleForTesting
import com.google.gson.Gson
import uk.co.glass_software.android.boilerplate.log.Logger
import uk.co.glass_software.android.shared_preferences.StoreEntryFactory
import java.util.*

class CacheInterceptorBuilder<E> internal constructor()
        where E : Exception,
              E : (E) -> Boolean {

    private var databaseName: String? = null
    private var logger: Logger? = null
    private var gson: Gson? = null

    fun gson(gson: Gson) = apply { this.gson = gson }
    fun databaseName(databaseName: String) = apply { this.databaseName = databaseName }
    fun logger(logger: Logger) = apply { this.logger = logger }

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

    @RestrictTo(RestrictTo.Scope.TESTS)
    internal fun build(context: Context,
                       holder: Holder<E>? = null): CacheInterceptor.Factory<E> {
        val gson = gson ?: Gson()

        val logger = this.logger ?: object : Logger {
            override fun d(message: String) = Unit
            override fun d(tag: String, message: String) = Unit
            override fun e(message: String) = Unit
            override fun e(tag: String, message: String) = Unit
            override fun e(tag: String, t: Throwable, message: String?) = Unit
            override fun e(t: Throwable, message: String?) = Unit
        }

        val storeEntryFactory = StoreEntryFactory.builder(context)
                .customSerialiser(GsonSerialiser(gson))
                .logger(logger)
                .build()

        val serialisationManager = SerialisationManager<E>(
                logger,
                storeEntryFactory,
                gson
        )

        val database = SqlOpenHelper(
                context.applicationContext,
                databaseName ?: DATABASE_NAME
        ).writableDatabase

        val dateFactory = { timeStamp: Long? -> timeStamp?.let { Date(it) } ?: Date() }

        val databaseManager = DatabaseManager(
                database,
                serialisationManager,
                logger,
                dateFactory,
                this::mapToContentValues
        )

        val cacheManager = CacheManager(
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
