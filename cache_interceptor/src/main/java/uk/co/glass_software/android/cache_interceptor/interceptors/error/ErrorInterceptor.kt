package uk.co.glass_software.android.cache_interceptor.interceptors.error

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.Function
import uk.co.glass_software.android.boilerplate.log.Logger
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import java.util.*
import java.util.concurrent.TimeUnit

internal class ErrorInterceptor<E> private constructor(private val errorFactory: (Throwable) -> E,
                                                       private val logger: Logger,
                                                       private val instructionToken: CacheToken,
                                                       private val timeOutInSeconds: Int)
    : ObservableTransformer<Any, ResponseWrapper<E>>
        where E : Exception,
              E : (E) -> Boolean {

    override fun apply(upstream: Observable<Any>) = upstream
            .filter { it != null } //see https://github.com/square/retrofit/issues/2242
            .switchIfEmpty(Observable.error(NoSuchElementException("Response was empty")))
            .timeout(timeOutInSeconds.toLong(), TimeUnit.SECONDS) //fixing timeout not working in OkHttp
            .map {
                ResponseWrapper<E>(
                        instructionToken.instruction.responseClass,
                        it,
                        CacheMetadata(instructionToken, null)
                )
            }
            .onErrorResumeNext(Function { Observable.just(getErrorResponse(it)) })!!

    private fun getErrorResponse(throwable: Throwable): ResponseWrapper<E> {
        val apiError = errorFactory(throwable)
        val responseClass = instructionToken.instruction.responseClass

        logger.e(
                apiError,
                "An error occurred during the network request for $responseClass"
        )

        return ResponseWrapper(
                responseClass,
                null,
                CacheMetadata(instructionToken, apiError)
        )
    }

    class Factory<E>(private val errorFactory: (Throwable) -> E,
                     private val logger: Logger,
                     private val timeOutInSeconds: Int)
            where E : Exception,
                  E : (E) -> Boolean {

        fun create(instructionToken: CacheToken) = ErrorInterceptor(
                errorFactory,
                logger,
                instructionToken,
                timeOutInSeconds
        )
    }
}
