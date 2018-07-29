package uk.co.glass_software.android.cache_interceptor.interceptors.error

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import io.reactivex.functions.Function
import javolution.util.stripped.FastMap.logger
import uk.co.glass_software.android.cache_interceptor.R
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.DoNotCache
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import uk.co.glass_software.android.shared_preferences.utils.Logger
import java.util.*
import java.util.concurrent.TimeUnit

class ErrorInterceptor<E> private constructor(private val errorFactory: (Throwable) -> E,
                                              private val logger: Logger,
                                              private val instructionToken: CacheToken)
    : ObservableTransformer<Any, ResponseWrapper<E>>
        where E : Exception,
              E : (E) -> Boolean {

    override fun apply(upstream: Observable<Any>): ObservableSource<ResponseWrapper<E>> {
        return upstream
                .filter { o -> o != null } //see https://github.com/square/retrofit/issues/2242
                .switchIfEmpty(Observable.error(NoSuchElementException("Response was empty")))
                .timeout(30, TimeUnit.SECONDS) //fixing timeout not working in OkHttp
                .map { ResponseWrapper<E>(instructionToken.instruction.responseClass, it) }
                .onErrorResumeNext(Function { Observable.just(getErrorResponse(it)) })
    }

    private fun getErrorResponse(throwable: Throwable): ResponseWrapper<E> {
        val apiError = errorFactory(throwable)
        val responseClass = instructionToken.instruction.responseClass

        logger.e(
                this,
                apiError,
                "An error occurred during the network request for $responseClass"
        )

        val instructionToken = CacheToken.fromInstruction(
                CacheInstruction(responseClass, DoNotCache),
                instructionToken.apiUrl,
                instructionToken.body
        )

        return ResponseWrapper(
                responseClass,
                null,
                CacheMetadata(instructionToken, apiError)
        )
    }

    class Factory<E>(private val errorFactory: (Throwable) -> E,
                     private val logger: Logger)
            where E : Exception,
                  E : (E) -> Boolean {

        fun create(instructionToken: CacheToken) = ErrorInterceptor(
                errorFactory,
                logger,
                instructionToken
        )
    }
}
