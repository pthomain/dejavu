package uk.co.glass_software.android.cache_interceptor.interceptors

import uk.co.glass_software.android.boilerplate.log.Logger
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor

class RxCacheInterceptorFactory<E> internal constructor(private val errorInterceptorFactory: ErrorInterceptor.Factory<E>,
                                                        private val cacheInterceptorFactory: CacheInterceptor.Factory<E>,
                                                        logger: Logger,
                                                        private val isCacheEnabled: Boolean)
        where E : Exception,
              E : (E) -> Boolean {

    private val responseInterceptor: ResponseInterceptor<E> = ResponseInterceptor(logger)

    internal fun create(responseClass: Class<*>,
                        instruction: CacheInstruction,
                        url: String,
                        body: String?) = RxCacheInterceptor(
            isCacheEnabled,
            responseClass,
            instruction,
            url,
            body,
            responseInterceptor,
            errorInterceptorFactory,
            cacheInterceptorFactory
    ) as RxCacheTransformer
}