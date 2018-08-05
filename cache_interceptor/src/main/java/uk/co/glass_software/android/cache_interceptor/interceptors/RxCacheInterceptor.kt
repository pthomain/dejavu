package uk.co.glass_software.android.cache_interceptor.interceptors

import android.content.Context
import com.google.gson.Gson
import io.reactivex.*
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.DoNotCache
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiErrorFactory
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor
import uk.co.glass_software.android.shared_preferences.utils.Logger

internal class RxCacheInterceptor<E> constructor(private val isCacheEnabled: Boolean,
                                                 val responseClass: Class<*>,
                                                 private val instruction: CacheInstruction,
                                                 private val url: String,
                                                 private val body: String?,
                                                 private val logger: Logger,
                                                 private val responseDecorator: ResponseDecorator<E>,
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

        val composedObservable = observable
                .compose(errorInterceptorFactory.create(instructionToken))
                .compose(cacheInterceptorFactory.create(instructionToken))

        val wrappedObservable = composedObservable
                .flatMap {
                    if (it.metadata?.exception == null) Observable.just(it)
                    else Observable.error(it.metadata?.exception)
                }

        return wrappedObservable.compose(responseDecorator)
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
