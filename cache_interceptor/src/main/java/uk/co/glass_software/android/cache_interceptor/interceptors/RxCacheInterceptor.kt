package uk.co.glass_software.android.cache_interceptor.interceptors

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.DoNotCache
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.cache_interceptor.configuration.CacheConfiguration
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.ResponseInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.CacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken.Companion.fromInstruction
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ErrorInterceptor

class RxCacheInterceptor<E> private constructor(instruction: CacheInstruction,
                                                url: String,
                                                uniqueParameters: String?,
                                                configuration: CacheConfiguration<E>,
                                                private val responseInterceptor: (Long) -> ResponseInterceptor<E>,
                                                private val errorInterceptorFactory: (CacheToken, Long) -> ErrorInterceptor<E>,
                                                private val cacheInterceptorFactory: (CacheToken, Long) -> CacheInterceptor<E>)
    : RxCacheTransformer
        where E : Exception,
              E : NetworkErrorProvider {

    private val instructionToken = fromInstruction(
            if (configuration.isCacheEnabled) instruction else instruction.copy(operation = DoNotCache),
            (instruction.operation as? Expiring)?.compress ?: false,
            (instruction.operation as? Expiring)?.encrypt ?: false,
            url,
            uniqueParameters
    )

    override fun apply(upstream: Observable<Any>): Observable<Any> {
        val start = System.currentTimeMillis()
        return upstream
                .compose(errorInterceptorFactory(instructionToken, start))
                .compose(cacheInterceptorFactory(instructionToken, start))
                .compose(responseInterceptor(start))!!
    }

    override fun apply(upstream: Single<Any>) = upstream
            .toObservable()
            .compose(this)
            .firstOrError()!!

    override fun apply(upstream: Completable) =
            cacheInterceptorFactory(
                    instructionToken,
                    System.currentTimeMillis()
            ).complete()

    class Factory<E> internal constructor(private val errorInterceptorFactory: (CacheToken, Long) -> ErrorInterceptor<E>,
                                          private val cacheInterceptorFactory: (CacheToken, Long) -> CacheInterceptor<E>,
                                          private val responseInterceptor: (Long) -> ResponseInterceptor<E>,
                                          private val configuration: CacheConfiguration<E>)
            where E : Exception,
                  E : NetworkErrorProvider {

        fun create(instruction: CacheInstruction,
                   url: String,
                   uniqueParameters: String?) =
                RxCacheInterceptor(
                        instruction,
                        url,
                        uniqueParameters,
                        configuration,
                        responseInterceptor,
                        errorInterceptorFactory,
                        cacheInterceptorFactory
                ) as RxCacheTransformer

    }
}
