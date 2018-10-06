package uk.co.glass_software.android.cache_interceptor.demo.presenter.volley

import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import uk.co.glass_software.android.boilerplate.Boilerplate.context
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Clear
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Expiring.Cache
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Expiring.Refresh
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse
import uk.co.glass_software.android.cache_interceptor.demo.presenter.BaseDemoPresenter

internal class VolleyDemoPresenter(demoActivity: DemoActivity,
                                   uiLogger: Logger)
    : BaseDemoPresenter(demoActivity, uiLogger) {

    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)

    companion object {
        private const val URL = BaseDemoPresenter.BASE_URL + BaseDemoPresenter.ENDPOINT
    }

    override fun getResponseObservable(isRefresh: Boolean,
                                       encrypt: Boolean,
                                       compress: Boolean,
                                       freshOnly: Boolean) =
            when {
                isRefresh -> Refresh(freshOnly = freshOnly)
                else -> Cache(
                        encrypt = encrypt,
                        compress = compress,
                        freshOnly = freshOnly
                )
            }.let { getObservableForOperation(it) }

    private fun getObservableForOperation(cacheOperation: Operation) =
            CacheInstruction(
                    CatFactResponse::class.java,
                    cacheOperation
            ).let { instruction ->
                rxCache.rxCacheInterceptor.create(
                        instruction,
                        URL,
                        null
                )
            }.let { interceptor ->
                VolleyObservable.createDefault(
                        requestQueue,
                        gson,
                        CatFactResponse::class.java,
                        interceptor,
                        URL
                )
            }

    override fun getClearEntriesCompletable() = getObservableForOperation(Clear()).ignoreElements()!!

    override fun getInvalidateCompletable() = getObservableForOperation(Operation.Invalidate).ignoreElements()!!

}