package uk.co.glass_software.android.cache_interceptor.interceptors

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Single
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.DoNotCache
import uk.co.glass_software.android.cache_interceptor.configuration.CacheConfiguration
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.ResponseInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.CacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ErrorInterceptor

class RxCacheInterceptor<E> private constructor(instruction: CacheInstruction,
                                                url: String,
                                                uniqueParameters: String?,
                                                configuration: CacheConfiguration<E>,
                                                private val responseInterceptor: ResponseInterceptor<E>,
                                                private val errorInterceptorFactory: (CacheToken) -> ErrorInterceptor<E>,
                                                private val cacheInterceptorFactory: (CacheToken) -> CacheInterceptor<E>)
    : RxCacheTransformer
        where E : Exception,
              E : NetworkErrorProvider {

    private val instructionToken = CacheToken.fromInstruction(
            if (configuration.isCacheEnabled) instruction else instruction.copy(operation = DoNotCache),
            url,
            uniqueParameters
    )

    override fun apply(observable: Observable<Any>): ObservableSource<Any> {
        return observable
                .compose(errorInterceptorFactory(instructionToken))
                .compose(cacheInterceptorFactory(instructionToken))
                .compose(responseInterceptor)
    }

    override fun apply(upstream: Single<Any>) = upstream
            .toObservable()
            .compose(this)
            .firstOrError()!!

    override fun apply(upstream: Completable) =
            cacheInterceptorFactory(instructionToken).complete()

    class Factory<E> internal constructor(private val errorInterceptorFactory: (CacheToken) -> ErrorInterceptor<E>,
                                          private val cacheInterceptorFactory: (CacheToken) -> CacheInterceptor<E>,
                                          private val responseInterceptor: ResponseInterceptor<E>,
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
