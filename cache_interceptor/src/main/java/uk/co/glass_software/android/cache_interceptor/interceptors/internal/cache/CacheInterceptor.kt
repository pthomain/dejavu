package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.*
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import java.util.*

internal class CacheInterceptor<E> constructor(private val cacheManager: CacheManager<E>,
                                               private val isCacheEnabled: Boolean,
                                               private val logger: Logger,
                                               private val instructionToken: CacheToken,
                                               private val start: Long)
    : ObservableTransformer<ResponseWrapper<E>, ResponseWrapper<E>>
        where E : Exception,
              E : NetworkErrorProvider {

    override fun apply(upstream: Observable<ResponseWrapper<E>>): ObservableSource<ResponseWrapper<E>> {
        val instruction = instructionToken.instruction

        val observable = if (isCacheEnabled) {
            when (instruction.operation) {
                is Expiring -> cacheManager.getCachedResponse(
                        upstream,
                        instructionToken,
                        instruction.operation,
                        start
                )
                else -> doNotCache(instructionToken, upstream)
            }
        } else doNotCache(instructionToken, upstream)

        return observable.filter {
            val filterFinal = (instruction.operation as? Expiring)?.filterFinal ?: false
            !filterFinal || it.metadata.cacheToken.status.isFinal
        }
    }

    fun complete() = (
            if (isCacheEnabled) {
                when (instructionToken.instruction.operation) {
                    is Clear -> clear(instructionToken.instruction.operation)
                    is Invalidate -> invalidate()
                    else -> Completable.complete()
                }
            } else Completable.complete()
            )!!

    private fun clear(operation: Clear) =
            cacheManager.clearCache(
                    operation.typeToClear,
                    operation.clearOldEntriesOnly
            )

    private fun invalidate() = cacheManager.invalidate(instructionToken)

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

}