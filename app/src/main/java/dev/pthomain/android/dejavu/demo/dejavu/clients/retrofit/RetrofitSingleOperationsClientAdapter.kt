package dev.pthomain.android.dejavu.demo.dejavu.clients.retrofit

import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.demo.dejavu.clients.base.adapters.SingleOperationsClientAdapter

class RetrofitSingleOperationsClientAdapter(
        private val singleClient: RetrofitSingleClients.Operations
) : SingleOperationsClientAdapter(singleClient), RetrofitObservableClients.Operations {

    override fun execute(operation: Operation)=
            singleClient.execute(operation).toObservable()

}