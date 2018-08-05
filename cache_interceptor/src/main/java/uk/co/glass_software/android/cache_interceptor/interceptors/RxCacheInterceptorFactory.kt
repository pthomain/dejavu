package uk.co.glass_software.android.cache_interceptor.interceptors

import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor
import uk.co.glass_software.android.shared_preferences.utils.Logger

class RxCacheInterceptorFactory<E> internal constructor(private val errorInterceptorFactory: ErrorInterceptor.Factory<E>,
                                                        private val cacheInterceptorFactory: CacheInterceptor.Factory<E>,
                                                        private val logger: Logger,
                                                        private val isCacheEnabled: Boolean)
        where E : Exception,
              E : (E) -> Boolean {

    fun create(responseClass: Class<*>,
               instruction: CacheInstruction,
               url: String,
               body: String?) = RxCacheInterceptor(
            isCacheEnabled,
            responseClass,
            instruction,
            url,
            body,
            logger,
            ResponseDecorator(),
            errorInterceptorFactory,
            cacheInterceptorFactory
    ) as RxCacheTransformer
}