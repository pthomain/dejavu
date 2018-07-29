package uk.co.glass_software.android.cache_interceptor.retrofit

import android.content.Context
import com.google.gson.Gson
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.cache_interceptor.annotations.AnnotationHelper
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import uk.co.glass_software.android.shared_preferences.utils.Logger
import uk.co.glass_software.android.shared_preferences.utils.SimpleLogger

class RetrofitCacheAdapterFactoryBuilder<E> internal constructor(private val context: Context,
                                                                 private val errorFactory: (Throwable) -> E)
        where E : Exception,
              E : (E) -> Boolean {

    private var gson: Gson? = null
    private var logger: Logger? = null
    private var isCacheEnabled = true
    private var useStrictMode = false

    fun gson(gson: Gson): RetrofitCacheAdapterFactoryBuilder<E> {
        this.gson = gson
        return this
    }

    fun logger(logger: Logger): RetrofitCacheAdapterFactoryBuilder<E> {
        this.logger = logger
        return this
    }

    fun cache(isCacheEnabled: Boolean): RetrofitCacheAdapterFactoryBuilder<E> {
        this.isCacheEnabled = isCacheEnabled
        return this
    }

    fun build(): RetrofitCacheAdapterFactory<E> {
        val cacheInterceptorFactory = RxCacheInterceptor.builder<E>()
                .gson(gson ?: Gson())
                .logger(logger ?: SimpleLogger())
                .errorFactory(errorFactory)
                .cache(isCacheEnabled)
                .build(context)

        return RetrofitCacheAdapterFactory(
                RxJava2CallAdapterFactory.create(),
                cacheInterceptorFactory,
                AnnotationHelper()
        )
    }
}
