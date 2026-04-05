package dev.pthomain.android.dejavu.volley

import com.android.volley.RequestQueue
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.response.HasMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.PlainRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.error.NetworkErrorPredicate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

/**
 * Provides convenience methods for getting a single result from a Volley Flow.
 * With coroutines, "single" semantics are achieved using Flow.first().
 */
class VolleySingle<E> private constructor()
        where E : Throwable,
              E : NetworkErrorPredicate {

    class Factory<E> internal constructor(
            private val flowFactory: VolleyFlowFactory.Factory<E>
    ) where E : Throwable,
            E : NetworkErrorPredicate {

        @Suppress("UNCHECKED_CAST") // This is enforced by DejaVuInterceptor
        suspend fun <R : Any> createResult(
                requestQueue: RequestQueue,
                operation: Operation,
                requestMetadata: PlainRequestMetadata<R>
        ): DejaVuResult<R> = flowFactory.createResult(
                requestQueue,
                operation,
                requestMetadata
        ).filter { (it as? HasMetadata<*, *, *>)?.cacheToken?.status?.isFinal ?: true }
                .first()

        @Suppress("UNCHECKED_CAST") // This is enforced by DejaVuInterceptor
        suspend fun <R : Any> create(
                requestQueue: RequestQueue,
                operation: Operation,
                requestMetadata: PlainRequestMetadata<R>
        ): R = flowFactory.create(
                requestQueue,
                operation,
                requestMetadata
        ).filter { (it as? HasMetadata<*, *, *>)?.cacheToken?.status?.isFinal ?: true }
                .first()

    }

}
