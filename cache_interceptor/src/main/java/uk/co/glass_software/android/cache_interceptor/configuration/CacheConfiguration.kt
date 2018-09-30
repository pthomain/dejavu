package uk.co.glass_software.android.cache_interceptor.configuration

import android.content.Context
import com.google.gson.Gson
import uk.co.glass_software.android.boilerplate.Boilerplate
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.BuildConfig
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiErrorFactory

class CacheConfiguration<E> private constructor(internal var context: Context,
                                                internal val logger: Logger,
                                                internal val errorFactory: ErrorFactory<E>,
                                                internal val databaseName: String,
                                                internal val gson: Gson,
                                                internal val isCacheEnabled: Boolean,
                                                internal val encrypt: Boolean,
                                                internal val compress: Boolean,
                                                internal val mergeOnNextOnError: Boolean,
                                                internal val networkTimeOutInSeconds: Int,
                                                internal val cacheDurationInMillis: Long)
        where E : Exception,
              E : NetworkErrorProvider {

    companion object {

        fun builder() = builder(ApiErrorFactory())

        fun <E> builder(errorFactory: ErrorFactory<E>)
                where E : Exception,
                      E : NetworkErrorProvider = Builder(errorFactory)

    }

    class Builder<E> internal constructor(private val errorFactory: ErrorFactory<E>)
            where E : Exception,
                  E : NetworkErrorProvider {

        private var logger: Logger? = null
        private var gson: Gson? = null

        private var databaseName: String = "rx_cache_interceptor.db"
        private var networkTimeOutInSeconds: Int = 15
        private var cacheDurationInMillis: Long = 60 * 60 * 1000 //1h

        private var isCacheEnabled = true
        private var compressData: Boolean = false
        private var encryptData: Boolean = false
        private var mergeOnNextOnError: Boolean = false

        fun noLog() = logger(object : Logger {
            override fun d(message: String) = Unit
            override fun d(tag: String, message: String) = Unit
            override fun e(message: String) = Unit
            override fun e(tag: String, message: String) = Unit
            override fun e(tag: String, t: Throwable, message: String?) = Unit
            override fun e(t: Throwable, message: String?) = Unit
        })

        fun logger(logger: Logger) = apply { this.logger = logger }

        fun gson(gson: Gson) = apply { this.gson = gson }

        fun databaseName(databaseName: String) = apply {
            if (!databaseName.isBlank()) {
                this.databaseName = if (databaseName.endsWith(".db")) databaseName else "$databaseName.db"
            }
        }

        fun setCacheEnabled(isCacheEnabled: Boolean) = apply { this.isCacheEnabled = isCacheEnabled }

        fun compressData(compressData: Boolean) = apply { this.compressData = compressData }

        fun encryptData(encryptData: Boolean) = apply { this.encryptData = encryptData }

        fun mergeOnNextOnError(mergeOnNextOnError: Boolean) = apply { this.mergeOnNextOnError = mergeOnNextOnError }

        fun networkTimeOutInSeconds(networkTimeOutInSeconds: Int) = apply { this.networkTimeOutInSeconds = networkTimeOutInSeconds }

        fun cacheDurationInMillis(cacheDurationInMillis: Long) = apply { this.cacheDurationInMillis = cacheDurationInMillis }

        fun build(context: Context): CacheConfiguration<E> {
            val logger = logger
                    ?: Boilerplate.init(context, BuildConfig.DEBUG).let { Boilerplate.logger }

            return CacheConfiguration(
                    context.applicationContext,
                    logger,
                    errorFactory,
                    databaseName,
                    gson ?: Gson(),
                    isCacheEnabled,
                    encryptData,
                    compressData,
                    mergeOnNextOnError,
                    networkTimeOutInSeconds,
                    cacheDurationInMillis
            )
        }
    }

}