package dev.pthomain.android.dejavu.demo.presenter.base

import dev.pthomain.android.boilerplate.core.utils.rx.observable
import dev.pthomain.android.boilerplate.core.utils.rx.single
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.response.Empty
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.response.Result
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.*
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.NetworkPriority.*
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.demo.dejavu.clients.model.CatFactResponse
import io.reactivex.Observable
import io.reactivex.Single

internal class OperationPresenterDelegate(
        private val executor: (Operation) -> Observable<DejaVuResult<CatFactResponse>>
) {

    fun getDataObservable(
            cachePriority: CachePriority,
            encrypt: Boolean,
            compress: Boolean
    ) =
            executeOperation(Operation.Remote.Cache(
                    priority = cachePriority,
                    encrypt = encrypt,
                    compress = compress
            )).flatMap {
                when (it) {
                    is Response<CatFactResponse, *> -> it.response.observable()
                    is Empty<*, *, *> -> Observable.error(it.exception)
                    is Result<*, *> -> Observable.empty()
                }
            }

    fun getOfflineSingle(freshness: FreshnessPriority) =
            executeOperation(
                    Operation.Remote.Cache(priority = CachePriority.with(LOCAL_ONLY, freshness))
            ).firstOrError().flatMap {
                when (it) {
                    is Response<CatFactResponse, *> -> it.response.single()
                    is Empty<*, *, *> -> Single.error(it.exception)
                    is Result<*, *> -> Single.error(NoSuchElementException(
                            "This operation does not emit any response: ${it.cacheToken.instruction.operation.type}")
                    )
                }
            }

    fun getClearEntriesResult() =
            executeOperation(Operation.Local.Clear())

    fun getInvalidateResult() =
            executeOperation(Operation.Local.Invalidate)

    fun executeOperation(cacheOperation: Operation) =
            executor(cacheOperation)

}