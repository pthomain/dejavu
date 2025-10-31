package dev.pthomain.android.dejavu.volley

import dev.pthomain.android.dejavu.cache.metadata.response.HasMetadata
import dev.pthomain.android.glitchy.core.interceptor.interceptors.error.NetworkErrorPredicate
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.SingleSource

class VolleySingle<E> private constructor(delegate: Single<Any>)
    : SingleSource<Any> by delegate
        where E : Throwable,
              E : NetworkErrorPredicate {

    class Factory<E> internal constructor(
            private val observableFactory: VolleyObservable.Factory<E>
    ) : ObservableTransformer<Any, Any>
            where E : Throwable,
                  E : NetworkErrorPredicate {

        override fun apply(upstream: Observable<Any>) = upstream.filter {
            (it as? HasMetadata<*, *, *>)?.cacheToken?.status?.isFinal ?: true
        }

        //FIXME
//        @Suppress("UNCHECKED_CAST") // This is enforced by DejaVuInterceptor
//        fun <R : Any> createResult(
//                requestQueue: RequestQueue,
//                operation: Operation,
//                requestMetadata: PlainRequestMetadata<R>
//        ) = observableFactory.createResult(
//                        requestQueue,
//                        operation,
//                        requestMetadata
//                )
//                .compose(this)
//                .firstOrError() as Single<DejaVuResult<R>>
//
//        @Suppress("UNCHECKED_CAST") // This is enforced by DejaVuInterceptor
//        fun <R : Any> create(
//                requestQueue: RequestQueue,
//                operation: Operation,
//                requestMetadata: PlainRequestMetadata<R>
//        ) = observableFactory.create(
//                        requestQueue,
//                        operation,
//                        requestMetadata
//                ).compose(this)
//                .firstOrError() as Single<R>

    }

}