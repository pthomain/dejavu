package uk.co.glass_software.android.cache_interceptor.interceptors

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.SingleTransformer
import uk.co.glass_software.android.boilerplate.log.Logger
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper

@Suppress("UNCHECKED_CAST")
internal class ResponseInterceptor<E>(private val logger: Logger)
    : ObservableTransformer<ResponseWrapper<E>, Any>,
        SingleTransformer<ResponseWrapper<E>, Any>
        where E : Exception,
              E : (E) -> Boolean {

    override fun apply(upstream: Observable<ResponseWrapper<E>>) = upstream.flatMap(this::intercept)!!

    override fun apply(upstream: Single<ResponseWrapper<E>>) = upstream.flatMapObservable(this::intercept).firstOrError()!!

    private fun intercept(wrapper: ResponseWrapper<E>): Observable<Any> {
        val response = wrapper.response
        val responseClass = wrapper.responseClass
        val metadata = wrapper.metadata!!
        val operation = metadata.cacheToken?.instruction?.operation

        return (response ?: createEmptyResponse(responseClass)).let {
            if (it != null) {
                addMetadata(
                        it,
                        responseClass,
                        metadata,
                        operation
                )
                Observable.just(it)
            } else {
                val exception = metadata.exception ?: IllegalStateException("No error available")
                logError(responseClass, operation)
                Observable.error(exception)
            }
        }
    }

    private fun createEmptyResponse(responseClass: Class<*>) = try {
        responseClass.newInstance()
    } catch (e: Exception) {
        null
    }

    private fun addMetadata(response: Any,
                            responseClass: Class<*>,
                            metadata: CacheMetadata<E>,
                            operation: CacheInstruction.Operation?) {
        val holder = response as? CacheMetadata.Holder<E>?
        if (holder != null) {
            holder.metadata = metadata
        } else {
            logError(responseClass, operation)
        }
    }

    private fun logError(responseClass: Class<*>,
                         operation: CacheInstruction.Operation?) {
        val message = "Could not add cache metadata to response '${responseClass.simpleName}'." +
                " If you want to enable metadata for this class, have it extend the" +
                " 'CacheMetadata.Holder' interface." +
                " The 'mergeOnNextOnError' directive will be ignored for classes" +
                " that do not support cache metadata."

        if (operation is Expiring && operation.mergeOnNextOnError) {
            logger.e(message)
        } else {
            logger.d(message)
        }
    }
}
