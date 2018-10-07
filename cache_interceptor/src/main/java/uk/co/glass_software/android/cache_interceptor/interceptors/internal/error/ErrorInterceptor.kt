package uk.co.glass_software.android.cache_interceptor.interceptors.internal.error

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.Function
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.configuration.ErrorFactory
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import java.util.*
import java.util.concurrent.TimeUnit

internal class ErrorInterceptor<E> constructor(private val errorFactory: ErrorFactory<E>,
                                               private val logger: Logger,
                                               private val instructionToken: CacheToken,
                                               private val start: Long,
                                               private val timeOutInSeconds: Int)
    : ObservableTransformer<Any, ResponseWrapper<E>>
        where E : Exception,
              E : NetworkErrorProvider {

    override fun apply(upstream: Observable<Any>) = upstream
            .filter { it != null } //see https://github.com/square/retrofit/issues/2242
            .switchIfEmpty(Observable.error(NoSuchElementException("Response was empty")))
            .timeout(timeOutInSeconds.toLong(), TimeUnit.SECONDS) //fixing timeout not working in OkHttp
            .map {
                ResponseWrapper<E>(
                        instructionToken.instruction.responseClass,
                        it,
                        start,
                        CacheMetadata(
                                instructionToken,
                                null,
                                getCallDuration()
                        )
                )
            }
            .onErrorResumeNext(Function { Observable.just(getErrorResponse(it)) })!!

    private fun getErrorResponse(throwable: Throwable): ResponseWrapper<E> {
        val apiError = errorFactory.getError(throwable)
        val responseClass = instructionToken.instruction.responseClass

        logger.e(
                apiError,
                "An error occurred during the network request for $responseClass"
        )

        return ResponseWrapper(
                responseClass,
                null,
                start,
                CacheMetadata(
                        instructionToken,
                        apiError,
                        getCallDuration()
                )
        )
    }

    private fun getCallDuration() =
            CacheMetadata.Duration(
                    0,
                    (System.currentTimeMillis() - start).toInt(),
                    0
            )

}
