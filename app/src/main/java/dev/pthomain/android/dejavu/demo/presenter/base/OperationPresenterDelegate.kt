package dev.pthomain.android.dejavu.demo.presenter.base

import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.response.Empty
import dev.pthomain.android.dejavu.cache.metadata.response.Response
import dev.pthomain.android.dejavu.cache.metadata.response.Result
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.Behaviour.OFFLINE
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.FreshnessPriority
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.demo.dejavu.clients.model.CatFactResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf

internal class OperationPresenterDelegate(
        private val executor: (Operation) -> Flow<DejaVuResult<CatFactResponse>>
) {

    fun getDataFlow(
            cachePriority: CachePriority,
            encrypt: Boolean,
            compress: Boolean
    ): Flow<CatFactResponse> =
            executeOperation(Operation.Remote.Cache(
                    priority = cachePriority,
                    serialisation = when {
                        encrypt && compress -> "compress,encrypt"
                        encrypt -> "encrypt"
                        compress -> "compress"
                        else -> ""
                    }
            )).flatMapConcat {
                when (it) {
                    is Response<CatFactResponse, *> -> flowOf(it.response)
                    is Empty<*, *, *> -> flow { throw it.exception }
                    is Result<*, *> -> emptyFlow()
                }
            }

    fun getOfflineFlow(freshness: FreshnessPriority): Flow<CatFactResponse> =
            flow {
                val result = executeOperation(
                        Operation.Remote.Cache(priority = CachePriority.with(OFFLINE, freshness))
                ).first()

                when (result) {
                    is Response<CatFactResponse, *> -> emit(result.response)
                    is Empty<*, *, *> -> throw result.exception
                    is Result<*, *> -> throw NoSuchElementException(
                            "This operation does not emit any response: ${result.cacheToken.instruction.operation.type}")
                }
            }

    fun getClearEntriesResult() =
            executeOperation(Operation.Local.Clear())

    fun getInvalidateResult() =
            executeOperation(Operation.Local.Invalidate)

    fun executeOperation(cacheOperation: Operation) =
            executor(cacheOperation)

}
