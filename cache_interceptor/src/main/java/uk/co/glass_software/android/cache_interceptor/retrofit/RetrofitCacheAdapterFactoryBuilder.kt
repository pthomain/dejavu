package uk.co.glass_software.android.cache_interceptor.retrofit

import android.content.Context
import com.google.gson.Gson
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.boilerplate.Boilerplate
import uk.co.glass_software.android.boilerplate.log.Logger
import uk.co.glass_software.android.cache_interceptor.BuildConfig
import uk.co.glass_software.android.cache_interceptor.annotations.AnnotationHelper
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor

class RetrofitCacheAdapterFactoryBuilder<E> internal constructor(private val context: Context,
                                                                 private val errorFactory: (Throwable) -> E)
        where E : Exception,
              E : (E) -> Boolean {

    private var gson: Gson? = null
    private var logger: Logger? = null
    private var isCacheEnabled = true
    private var timeOutInSeconds: Int? = null

    fun gson(gson: Gson) = apply { this.gson = gson }

    fun logger(logger: Logger) = apply { this.logger = logger }

    fun cache(isCacheEnabled: Boolean) = apply { this.isCacheEnabled = isCacheEnabled }

    fun timeOutInSeconds(timeOutInSeconds: Int) = apply { this.timeOutInSeconds = timeOutInSeconds }

    fun build() = RxCacheInterceptor.builder<E>()
            .gson(gson ?: Gson())
            .logger(logger
                    ?: Boilerplate.init(context, BuildConfig.DEBUG).let { Boilerplate.logger }
            )
            .errorFactory(errorFactory)
            .setCacheEnabled(isCacheEnabled)
            .apply {
                timeOutInSeconds?.also { timeOutInSeconds(it) }
            }
            .build(context)
            .let {
                RetrofitCacheAdapterFactory(
                        RxJava2CallAdapterFactory.create(),
                        it,
                        AnnotationHelper()
                )
            }
}
