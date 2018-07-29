package uk.co.glass_software.android.cache_interceptor.interceptors

import android.content.Context
import com.google.gson.Gson
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptorBuilder.Companion.DATABASE_NAME
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor
import uk.co.glass_software.android.shared_preferences.utils.Logger
import uk.co.glass_software.android.shared_preferences.utils.SimpleLogger

class RxCacheInterceptorBuilder<E> internal constructor()
        where E : Exception,
              E : (E) -> Boolean {

    private var logger: Logger? = null
    private var errorFactory: ((Throwable) -> E)? = null
    private var databaseName: String? = null
    private var gson: Gson? = null
    private var compressData = true
    private var encryptData = false
    private var isCacheEnabled = true

    fun errorFactory(errorFactory: (Throwable) -> E): RxCacheInterceptorBuilder<E> {
        this.errorFactory = errorFactory
        return this
    }

    fun noLog(): RxCacheInterceptorBuilder<E> {
        return logger(object : Logger {
            override fun e(caller: Any, t: Throwable, message: String) {}
            override fun e(caller: Any, message: String) {}
            override fun d(caller: Any, message: String) {}
        })
    }

    fun logger(logger: Logger): RxCacheInterceptorBuilder<E> {
        this.logger = logger
        return this
    }

    fun gson(gson: Gson): RxCacheInterceptorBuilder<E> {
        this.gson = gson
        return this
    }

    fun databaseName(databaseName: String): RxCacheInterceptorBuilder<E> {
        this.databaseName = databaseName
        return this
    }

    fun cache(isCacheEnabled: Boolean): RxCacheInterceptorBuilder<E> {
        this.isCacheEnabled = isCacheEnabled
        return this
    }

    fun compress(compressData: Boolean): RxCacheInterceptorBuilder<E> {
        this.compressData = compressData
        return this
    }

    fun encrypt(encryptData: Boolean): RxCacheInterceptorBuilder<E> {
        this.encryptData = encryptData
        return this
    }

    fun build(context: Context) = build(context, null)

    internal fun build(context: Context,
                       holder: DependencyHolder?): RxCacheInterceptor.Factory<E> {
        val logger = logger ?: SimpleLogger()

        val errorInterceptorFactory = errorFactory?.let {
            ErrorInterceptor.Factory(
                    it,
                    logger
            )
        } ?: throw IllegalStateException("Please provide an error factory")

        val cacheInterceptorFactory = CacheInterceptor.builder<E>()
                .logger(logger)
                .databaseName(databaseName ?: DATABASE_NAME)
                .gson(gson ?: Gson())
                .build(context.applicationContext, compressData, encryptData)

        if (holder != null) {
            holder.gson = gson
            holder.errorFactory = this.errorFactory
            holder.errorInterceptorFactory = errorInterceptorFactory
            holder.cacheInterceptorFactory = cacheInterceptorFactory
        }

        return RxCacheInterceptor.Factory(
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
