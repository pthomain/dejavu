package uk.co.glass_software.android.cache_interceptor.interceptors.internal

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.SingleTransformer
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper

@Suppress("UNCHECKED_CAST")
internal class ResponseInterceptor<E>(private val logger: Logger,
                                      private val mergeOnNextOnError: Boolean)
    : ObservableTransformer<ResponseWrapper<E>, Any>,
        SingleTransformer<ResponseWrapper<E>, Any>
        where E : Exception,
              E : NetworkErrorProvider {

    override fun apply(upstream: Observable<ResponseWrapper<E>>): Observable<Any> {
        var start = 0L
        return upstream
                .doOnSubscribe { start = System.currentTimeMillis() }
                .doAfterNext { start = System.currentTimeMillis() }!!
                .flatMap { this.intercept(it, start) }
    }

    override fun apply(upstream: Single<ResponseWrapper<E>>) =
            upstream
                    .toObservable()
                    .compose(this)
                    .firstOrError()!!

    private fun intercept(wrapper: ResponseWrapper<E>,
                          start: Long): Observable<Any> {
        val responseClass = wrapper.responseClass
        val metadata = wrapper.metadata!!
        val operation = metadata.cacheToken.instruction.operation

        val mergeOnNextOnError = (operation as? Expiring)?.mergeOnNextOnError
                ?: this.mergeOnNextOnError

        val response = wrapper.response ?: createEmptyResponse(mergeOnNextOnError, responseClass)

        return if (response != null) {
            addMetadata(
                    response,
                    responseClass,
                    metadata,
                    operation,
                    mergeOnNextOnError,
                    start
            )
            logger.d("Returning response: $metadata")
            Observable.just(response)
        } else {
            val exception = metadata.exception ?: IllegalStateException("No error available")

            logError(
                    responseClass,
                    operation,
                    mergeOnNextOnError
            )

            logger.d("Returning error: $exception")
            Observable.error(exception)
        }
    }

    private fun createEmptyResponse(mergeOnNextOnError: Boolean,
                                    responseClass: Class<*>) =
            if (mergeOnNextOnError) {
                try {
                    responseClass.newInstance()
                } catch (e: Exception) {
                    null
                }
            } else null

    private fun addMetadata(response: Any,
                            responseClass: Class<*>,
                            metadata: CacheMetadata<E>,
                            operation: CacheInstruction.Operation?,
                            mergeOnNextOnError: Boolean,
                            start: Long) {
        val holder = response as? CacheMetadata.Holder<E>?
        if (holder != null) {
            holder.metadata = metadata.copy(callDuration = System.currentTimeMillis() - start)
        } else {
            logError(responseClass, operation, mergeOnNextOnError)
        }
    }

    private fun logError(responseClass: Class<*>,
                         operation: CacheInstruction.Operation?,
                         mergeOnNextOnError: Boolean) {
        val message = "Could not add cache metadata to response '${responseClass.simpleName}'." +
                " If you want to enable metadata for this class, it needs extend the" +
                " 'CacheMetadata.Holder' interface." +
                " The 'mergeOnNextOnError' directive will be ignored for classes" +
                " that do not support cache metadata."

        if (operation is Expiring && mergeOnNextOnError) {
            logger.e(message)
        } else {
            logger.d(message)
        }
    }
}
