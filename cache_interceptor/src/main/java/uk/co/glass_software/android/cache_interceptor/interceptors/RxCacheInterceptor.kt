package uk.co.glass_software.android.cache_interceptor.interceptors

import android.content.Context
import com.google.gson.Gson
import io.reactivex.*
import uk.co.glass_software.android.cache_interceptor.R
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.DoNotCache
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiErrorFactory
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorInterceptor
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import uk.co.glass_software.android.shared_preferences.utils.Logger

class RxCacheInterceptor<E> constructor(private val isCacheEnabled: Boolean,
                                        val responseClass: Class<*>,
                                        private val instruction: CacheInstruction,
                                        private val url: String,
                                        private val body: String?,
                                        private val logger: Logger,
                                        private val responseDecorator: ResponseDecorator,
                                        private val errorInterceptorFactory: ErrorInterceptor.Factory<E>,
                                        private val cacheInterceptorFactory: CacheInterceptor.Factory<E>)
    : ObservableTransformer<Any, Any>,
        SingleTransformer<Any, Any>
        where E : Exception,
              E : (E) -> Boolean {

    override fun apply(observable: Observable<Any>): ObservableSource<Any> {
        val instruction = if (isCacheEnabled) instruction else instruction.copy(operation = DoNotCache)

        val composedObservable = observable
                .compose(errorInterceptorFactory.create(responseClass))
                .compose(cacheInterceptorFactory.create(
                        responseClass,
                        instruction,
                        url,
                        body
                ))

        val wrappedObservable = if (instruction.mergeOnNextOnError)
            composedObservable
        else
            composedObservable
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

    class Factory<E> constructor(private val errorInterceptorFactory: ErrorInterceptor.Factory<E>,
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
        )
    }

    companion object {

        fun <E> builder(): RxCacheInterceptorBuilder<E>
                where E : Exception,
                      E : (E) -> Boolean = RxCacheInterceptorBuilder()

        fun buildDefault(context: Context): RxCacheInterceptor.Factory<ApiError> {
            return RxCacheInterceptor.builder<ApiError>()
                    .gson(Gson())
                    .errorFactory(ApiErrorFactory())
                    .build(context)
        }
    }
}
