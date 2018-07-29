package uk.co.glass_software.android.cache_interceptor.interceptors.cache

import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.ObservableTransformer
import uk.co.glass_software.android.cache_interceptor.R
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.DoNotCache
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.CACHE
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.REFRESH
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import uk.co.glass_software.android.shared_preferences.utils.Logger
import java.util.*

class CacheInterceptor<E> internal constructor(private val cacheManager: CacheManager<E>,
                                               private val isCacheEnabled: Boolean,
                                               private val logger: Logger,
                                               private val instructionToken: CacheToken,
                                               private val apiUrl: String,
                                               private val body: String?)
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
                        instruction,
                        instruction.operation,
                        apiUrl,
                        body,
                        isNetworkError
                )
                is CacheInstruction.Operation.Clear -> doNotCache(upstream) //TODO
                is CacheInstruction.Operation.DoNotCache -> doNotCache(upstream)
            }
        } else doNotCache(upstream)

        return observable.doOnNext { wrapper ->
            val instructionToken = CacheToken.fromInstruction(
                    instruction,
                    apiUrl,
                    body
            )
            if (wrapper.metadata!!.cacheToken!!.instruction.operation === DoNotCache) {
                wrapper.metadata = wrapper.metadata!!.copy(
                        cacheToken = CacheToken.notCached(
                                instructionToken,
                                Date()
                        )
                )
            }
            logger.d(this, "Returning: $instructionToken")
        }
    }

    private fun doNotCache(instructionToken: CacheToken,
                           upstream: Observable<ResponseWrapper<E>>): Observable<ResponseWrapper<E>> {
        return upstream.doOnNext { responseWrapper ->
            responseWrapper.metadata = CacheMetadata(
                    CacheToken.notCached(
                            instructionToken,
                            Date()
                    )
            )
        }
    }

    class Factory<E> internal constructor(private val cacheManager: CacheManager<E>,
                                          private val isCacheEnabled: Boolean,
                                          private val logger: Logger)
            where E : Exception,
                  E : (E) -> Boolean {

        fun create(instructionToken: CacheToken) = CacheInterceptor<E>(
                cacheManager,
                isCacheEnabled,
                logger,
                instructionToken
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