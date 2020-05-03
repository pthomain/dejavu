package dev.pthomain.android.dejavu.demo.presenter.volley

import com.android.volley.RequestQueue
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.cache.metadata.response.DejaVuResult
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.PlainRequestMetadata
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority.FreshnessPriority
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.demo.DemoActivity
import dev.pthomain.android.dejavu.demo.dejavu.DejaVuVolleyClient
import dev.pthomain.android.dejavu.demo.dejavu.clients.base.ObservableClients
import dev.pthomain.android.dejavu.demo.dejavu.clients.base.SingleClients
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.ErrorFactoryType
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.ErrorFactoryType.Custom
import dev.pthomain.android.dejavu.demo.dejavu.clients.factories.ErrorFactoryType.Default
import dev.pthomain.android.dejavu.demo.dejavu.clients.model.CatFactResponse
import dev.pthomain.android.dejavu.demo.dejavu.error.CustomApiError
import dev.pthomain.android.dejavu.demo.presenter.base.BaseDemoPresenter
import dev.pthomain.android.dejavu.demo.presenter.base.OperationPresenterDelegate
import dev.pthomain.android.glitchy.core.interceptor.error.glitch.Glitch
import io.reactivex.Observable

internal class VolleyPresenter(
        demoActivity: DemoActivity,
        uiLogger: Logger
) : BaseDemoPresenter<SingleClients.Operations, ObservableClients.Operations, DejaVuVolleyClient>(
        demoActivity,
        uiLogger
) {

    private val queue = RequestQueue(
            DiskBasedCache(context().cacheDir, 1024),
            BasicNetwork(HurlStack())
    ).apply { start() }

    private val delegate = OperationPresenterDelegate(::executeOperation)

    override fun newClient() = when (errorFactoryType) {
        Default -> dejaVuFactory.createVolley(errorFactoryType as ErrorFactoryType<Glitch>)
        Custom -> dejaVuFactory.createVolley(errorFactoryType as ErrorFactoryType<CustomApiError>)
    }

    override fun getDataObservable(
            cachePriority: CachePriority,
            encrypt: Boolean,
            compress: Boolean
    ) =
            delegate.getDataObservable(cachePriority, encrypt, compress)

    override fun getOfflineSingle(freshness: FreshnessPriority) =
            delegate.getOfflineSingle(freshness)

    override fun getClearEntriesResult() =
            delegate.getClearEntriesResult()

    override fun getInvalidateResult() =
            delegate.getInvalidateResult()

    private fun executeOperation(cacheOperation: Operation) =
            dejaVuClient.observableFactory.createResult(
                    queue,
                    cacheOperation,
                    PlainRequestMetadata(
                            CatFactResponse::class.java,
                            BASE_URL + ENDPOINT
                    )
            )
}