package uk.co.glass_software.android.cache_interceptor.interceptors

import android.content.Context
import com.google.gson.Gson
import io.reactivex.*
import uk.co.glass_software.android.boilerplate.log.Logger
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.DoNotCache
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiErrorFactory
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor

internal class RxCacheInterceptor<E> internal constructor(private val isCacheEnabled: Boolean,
                                                          val responseClass: Class<*>,
                                                          private val instruction: CacheInstruction,
                                                          private val url: String,
                                                          private val body: String?,
                                                          private val responseInterceptor: ResponseInterceptor<E>,
                                                          private val errorInterceptorFactory: ErrorInterceptor.Factory<E>,
                                                          private val cacheInterceptorFactory: CacheInterceptor.Factory<E>)
    : RxCacheTransformer
        where E : Exception,
              E : (E) -> Boolean {

    override fun apply(observable: Observable<Any>): ObservableSource<Any> {
        val instructionToken = CacheToken.fromInstruction(
                if (isCacheEnabled) instruction else instruction.copy(operation = DoNotCache),
                url,
                body
        )

        return observable
                .compose(errorInterceptorFactory.create(instructionToken))
                .compose(cacheInterceptorFactory.create(instructionToken))
                .compose(responseInterceptor)
    }

    override fun apply(upstream: Single<Any>) = upstream
            .toObservable()
            .compose(this)
            .firstOrError()!!

    companion object {

        fun <E> builder(): RxCacheInterceptorBuilder<E>
                where E : Exception,
                      E : (E) -> Boolean = RxCacheInterceptorBuilder()

        fun buildDefault(context: Context): RxCacheInterceptorFactory<ApiError> {
            return RxCacheInterceptor.builder<ApiError>()
                    .gson(Gson())
                    .errorFactory(ApiErrorFactory())
                    .build(context)
        }
    }
}
