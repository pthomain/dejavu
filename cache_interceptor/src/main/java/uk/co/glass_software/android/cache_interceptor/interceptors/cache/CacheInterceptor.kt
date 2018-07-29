package uk.co.glass_software.android.cache_interceptor.interceptors.cache

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.DoNotCache
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.CACHE
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.REFRESH
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import uk.co.glass_software.android.shared_preferences.utils.Logger
import java.util.*

class CacheInterceptor<E> internal constructor(private val cacheManager: CacheManager,
                                               private val isCacheEnabled: Boolean,
                                               private val logger: Logger,
                                               private val responseClass: Class<*>,
                                               private val instruction: CacheInstruction,
                                               private val apiUrl: String,
                                               private val body: String?)
    : ObservableTransformer<ResponseWrapper, ResponseWrapper>
        where E : Exception,
              E : (E) -> Boolean {

    override fun apply(upstream: Observable<ResponseWrapper>): ObservableSource<ResponseWrapper> {
        val responseToken = CacheToken(
                responseClass,
                instruction,
                CacheStatus.CACHED,
                apiUrl,
                body
        )
        val isNetworkError = { error: E -> error(error) }

        val isCacheOperation = instruction.operation.type.let { it == CACHE || it == REFRESH }
        val observable = if (isCacheEnabled && isCacheOperation)
            cacheManager.getCachedResponse(
                    upstream,
                    instruction,
                    instruction.operation,
                    apiUrl,
                    body,
                    isNetworkError
            )
        else
            doNotCache(upstream)

        return observable.doOnNext { (response, metadata) ->
            if (metadata.instruction.operation === DoNotCache) {
                metadata.responseToken = CacheToken.notCached(
                        metadata.getRequestToken(),
                        Date()
                )
            }
            logger.d(this, "Returning: " + cacheToken.toString())
        }
    }

    private fun doNotCache(upstream: Observable<ResponseWrapper>): Observable<ResponseWrapper> {
        return upstream.doOnNext { (_, metadata) ->
            metadata.responseToken = CacheToken.notCached(
                    cacheToken,
                    Date()
            )
        }
    }

    class Factory<E> internal constructor(private val cacheManager: CacheManager,
                                          private val isCacheEnabled: Boolean,
                                          private val logger: Logger)
            where E : Exception,
                  E : (E) -> Boolean {

        fun <R> create(responseClass: Class<R>,
                       instruction: CacheInstruction,
                       apiUrl: String,
                       body: String?) = CacheInterceptor<E, R>(
                cacheManager,
                isCacheEnabled,
                logger,
                responseClass,
                instruction,
                apiUrl,
                body
        )
    }

    companion object {
        fun <E> builder(): CacheInterceptorBuilder<E>
                where E : Exception,
                      E : (E) -> Boolean {
            return CacheInterceptorBuilder()
        }
    }
}