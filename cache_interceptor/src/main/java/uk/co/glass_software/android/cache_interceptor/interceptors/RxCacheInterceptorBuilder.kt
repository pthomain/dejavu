package uk.co.glass_software.android.cache_interceptor.interceptors

import android.content.Context
import com.google.gson.Gson
import uk.co.glass_software.android.boilerplate.Boilerplate
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.BuildConfig
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptorBuilder.Companion.DATABASE_NAME
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor

class RxCacheInterceptorBuilder<E> internal constructor()
        where E : Exception,
              E : (E) -> Boolean {

    private var logger: Logger? = null
    private var errorFactory: ((Throwable) -> E)? = null
    private var databaseName: String? = null
    private var gson: Gson? = null
    private var isCacheEnabled = true
    private var timeOutInSeconds = 30

    fun noLog() = logger(object : Logger {
        override fun d(message: String) = Unit
        override fun d(tag: String, message: String) = Unit
        override fun e(message: String) = Unit
        override fun e(tag: String, message: String) = Unit
        override fun e(tag: String, t: Throwable, message: String?) = Unit
        override fun e(t: Throwable, message: String?) = Unit
    })

    fun logger(logger: Logger) = apply { this.logger = logger }

    fun errorFactory(errorFactory: (Throwable) -> E) = apply { this.errorFactory = errorFactory }

    fun gson(gson: Gson) = apply { this.gson = gson }

    fun databaseName(databaseName: String) = apply { this.databaseName = databaseName }

    fun timeOutInSeconds(timeOutInSeconds: Int) = apply { this.timeOutInSeconds = timeOutInSeconds }

    fun setCacheEnabled(isCacheEnabled: Boolean) = apply { this.isCacheEnabled = isCacheEnabled }

    fun build(context: Context) = build(context, null)

    internal fun build(context: Context,
                       holder: DependencyHolder?): RxCacheInterceptorFactory<E> {
        val logger = logger
                ?: Boilerplate.init(context, BuildConfig.DEBUG).let { Boilerplate.logger }

        val errorInterceptorFactory = errorFactory?.let {
            ErrorInterceptor.Factory(
                    it,
                    logger,
                    timeOutInSeconds
            )
        } ?: throw IllegalStateException("Please provide an error factory")

        val cacheInterceptorFactory = CacheInterceptor.builder<E>()
                .logger(logger)
                .databaseName(databaseName ?: DATABASE_NAME)
                .gson(gson ?: Gson())
                .build(context.applicationContext)

        if (holder != null) {
            holder.gson = gson
            holder.errorFactory = this.errorFactory
            holder.errorInterceptorFactory = errorInterceptorFactory
            holder.cacheInterceptorFactory = cacheInterceptorFactory
        }

        return RxCacheInterceptorFactory(
                errorInterceptorFactory,
                cacheInterceptorFactory,
                logger,
                isCacheEnabled
        )
    }

    internal inner class DependencyHolder {
        var errorFactory: ((Throwable) -> E)? = null
        var gson: Gson? = null
        var errorInterceptorFactory: ErrorInterceptor.Factory<E>? = null
        var cacheInterceptorFactory: CacheInterceptor.Factory<E>? = null
    }
}
