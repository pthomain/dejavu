package uk.co.glass_software.android.cache_interceptor.configuration

import android.content.Context
import android.os.Looper
import com.google.gson.Gson
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.schedulers.AndroidSchedulers
import uk.co.glass_software.android.boilerplate.Boilerplate
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.BuildConfig.DEBUG
import uk.co.glass_software.android.cache_interceptor.RxCache
import uk.co.glass_software.android.cache_interceptor.injection.CacheComponent

data class CacheConfiguration<E> internal constructor(val context: Context,
                                                      val logger: Logger,
                                                      val errorFactory: ErrorFactory<E>,
                                                      val gson: Gson,
                                                      val isCacheEnabled: Boolean,
                                                      val encrypt: Boolean,
                                                      val compress: Boolean,
                                                      val mergeOnNextOnError: Boolean,
                                                      val networkTimeOutInSeconds: Int,
                                                      val cacheDurationInMillis: Long,
                                                      val cacheAllByDefault: Boolean)
        where E : Exception,
              E : NetworkErrorProvider {

    companion object {

        internal fun <E> builder(errorFactory: ErrorFactory<E>,
                                 componentProvider: (CacheConfiguration<E>) -> CacheComponent<E>)
                where E : Exception,
                      E : NetworkErrorProvider =
                Builder(errorFactory, componentProvider)
    }

    class Builder<E> internal constructor(
            private val errorFactory: ErrorFactory<E>,
            private val componentProvider: (CacheConfiguration<E>) -> CacheComponent<E>
    ) where E : Exception,
            E : NetworkErrorProvider {

        private var logger: Logger? = null
        private var gson: Gson? = null

        private var networkTimeOutInSeconds: Int = 15
        private var cacheDurationInMillis: Long = 60 * 60 * 1000 //1h

        private var isCacheEnabled = true
        private var compressData: Boolean = false
        private var encryptData: Boolean = false
        private var mergeOnNextOnError: Boolean = false
        private var cacheAllByDefault: Boolean = true

        /**
         * Disables log output (default log output is only enabled in DEBUG mode).
         */
        fun noLog() = logger(object : Logger {
            override fun d(message: String) = Unit
            override fun d(tag: String, message: String) = Unit
            override fun e(message: String) = Unit
            override fun e(tag: String, message: String) = Unit
            override fun e(tag: String, t: Throwable, message: String?) = Unit
            override fun e(t: Throwable, message: String?) = Unit
        })

        /**
         * Sets custom logger.
         */
        fun logger(logger: Logger) = apply { this.logger = logger }

        /**
         * Sets custom Gson implementation.
         */
        fun gson(gson: Gson) = apply { this.gson = gson }

        /**
         * Sets network call timeout in seconds globally (default is 15s).
         */
        fun networkTimeOutInSeconds(networkTimeOutInSeconds: Int) = apply { this.networkTimeOutInSeconds = networkTimeOutInSeconds }

        /**
         * Sets the global cache duration in milliseconds (used by default for all calls with no specific directive,
         * see @Cache::durationInMillis for call-specific directive).
         */
        fun cacheDurationInMillis(cacheDurationInMillis: Long) = apply { this.cacheDurationInMillis = cacheDurationInMillis }

        /**
         * Enables or disables cache globally, regardless of individual call setup.
         * Error handling is still executing and errors will be delivered in 2 possible ways:
         *
         * - as metadata on the response if the response implements CacheMetadata.Holder and
         * the mergeOnNextOnError directive is set to true for the call.
         *
         * - using the default RxJava error mechanism otherwise.
         */
        fun setCacheEnabled(isCacheEnabled: Boolean) = apply { this.isCacheEnabled = isCacheEnabled }

        /**
         * Sets the data compression globally (used by default for all calls with no specific directive,
         * see @Cache::compress for call-specific directive).
         */
        fun compressData(compressData: Boolean) = apply { this.compressData = compressData }

        /**
         * Sets the data encryption globally (used by default for all calls with no specific directive,
         * see @Cache::encrypt for call-specific directive).
         */
        fun encryptData(encryptData: Boolean) = apply { this.encryptData = encryptData }

        /**
         * Sets response/error merging globally (used by default for all calls with no specific directive,
         * see @Cache::mergeOnNextOnError for call-specific directive).
         *
         * When set to true, errors will be added as metadata to any call implementing
         * the CacheMetadata.Holder interface. This means onError(t:Throwable) will never be called.
         *
         * Instead if an error occurs, an empty response is returned with the exception available as
         * metadata. Special care must be taken to check if the response metadata contains an error
         * before attempting to read any of its fields.
         *
         * When used by mistake on a call returning a response that does not implement
         * CacheMetadata.Holder, this directive is ignored and the exception is delivered using the
         * default RxJava mechanism which may cause a crash if no uncaught error handler
         * is set and the onError(t:Throwable) callback is not provided.
         */
        fun mergeOnNextOnError(mergeOnNextOnError: Boolean) = apply { this.mergeOnNextOnError = mergeOnNextOnError }

        /**
         * Sets data caching globally (used by default for all calls with no annotation) using
         * the default global values set here. As for any global directive, it is overridden by
         * call-specific values.
         */
        fun cacheAllByDefault(cacheAllByDefault: Boolean) = apply { this.cacheAllByDefault = cacheAllByDefault }

        /**
         * Returns an instance of RxCache.
         */
        fun build(context: Context): RxCache<E> {
            val logger = logger
                    ?: Boilerplate.init(context, DEBUG).let { Boilerplate.logger }

            RxAndroidPlugins.setInitMainThreadSchedulerHandler {
                AndroidSchedulers.from(Looper.getMainLooper(), true)
            }

            return RxCache(
                    componentProvider(
                            CacheConfiguration(
                                    context.applicationContext,
                                    logger,
                                    errorFactory,
                                    gson ?: Gson(),
                                    isCacheEnabled,
                                    encryptData,
                                    compressData,
                                    mergeOnNextOnError,
                                    networkTimeOutInSeconds,
                                    cacheDurationInMillis,
                                    cacheAllByDefault
                            ).also { logger.d("RxCache set up with the following configuration: $it") }
                    )
            )
        }
    }

}