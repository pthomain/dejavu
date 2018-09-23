package uk.co.glass_software.android.cache_interceptor.interceptors.cache

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import uk.co.glass_software.android.boilerplate.log.Logger
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.DoNotCache
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import java.util.*

internal class CacheInterceptor<E> constructor(private val cacheManager: CacheManager<E>,
                                               private val isCacheEnabled: Boolean,
                                               private val logger: Logger,
                                               private val instructionToken: CacheToken)
    : ObservableTransformer<ResponseWrapper<E>, ResponseWrapper<E>>
        where E : Exception,
              E : (E) -> Boolean {

    override fun apply(upstream: Observable<ResponseWrapper<E>>): ObservableSource<ResponseWrapper<E>> {
        val isNetworkError = { error: E -> error(error) }
        val instruction = instructionToken.instruction

        val observable = if (isCacheEnabled) {
            when (instruction.operation) {
                is Expiring -> cacheManager.getCachedResponse(
                        upstream,
                        instructionToken,
                        instruction.operation,
                        isNetworkError
                )
                is CacheInstruction.Operation.Clear -> doNotCache(instructionToken, upstream) //TODO clear cache
                is CacheInstruction.Operation.DoNotCache -> doNotCache(instructionToken, upstream)
            }
        } else doNotCache(instructionToken, upstream)

        return observable.doOnNext { wrapper ->
            val metadata = wrapper.metadata!!
            if (metadata.cacheToken.instruction.operation === DoNotCache) {
                wrapper.metadata = metadata.copy(
                        cacheToken = CacheToken.notCached(
                                instructionToken,
                                Date()
                        )
                )
            }
            logger.d("Returning: $instructionToken")
        }
    }

    private fun doNotCache(instructionToken: CacheToken,
                           upstream: Observable<ResponseWrapper<E>>) =
            upstream.doOnNext { responseWrapper ->
                responseWrapper.metadata = CacheMetadata(
                        CacheToken.notCached(
                                instructionToken,
                                Date()
                        )
                )
            }

    class Factory<E> internal constructor(private val cacheManager: CacheManager<E>,
                                          private val isCacheEnabled: Boolean,
                                          private val logger: Logger)
            where E : Exception,
                  E : (E) -> Boolean {

        fun create(instructionToken: CacheToken) = CacheInterceptor(
                cacheManager,
                isCacheEnabled,
                logger,
                instructionToken
        )
    }

    companion object {
        fun <E> builder(): CacheInterceptorBuilder<E>
                where E : Exception,
                      E : (E) -> Boolean = CacheInterceptorBuilder()
    }
}