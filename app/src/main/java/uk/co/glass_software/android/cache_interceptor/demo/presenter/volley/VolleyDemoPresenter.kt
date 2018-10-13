package uk.co.glass_software.android.cache_interceptor.demo.presenter.volley

import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import uk.co.glass_software.android.boilerplate.Boilerplate.context
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Clear
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Expiring.*
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Invalidate
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse
import uk.co.glass_software.android.cache_interceptor.demo.presenter.BaseDemoPresenter

internal class VolleyDemoPresenter(demoActivity: DemoActivity,
                                   uiLogger: Logger)
    : BaseDemoPresenter(demoActivity, uiLogger) {

    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)
    private val responseClass = CatFactResponse::class.java

    companion object {
        private const val URL = BaseDemoPresenter.BASE_URL + BaseDemoPresenter.ENDPOINT
    }

    override fun getResponseObservable(isRefresh: Boolean,
                                       encrypt: Boolean,
                                       compress: Boolean,
                                       freshOnly: Boolean) =
            getObservableForOperation(when {
                isRefresh -> Refresh(freshOnly = freshOnly)
                else -> Cache(
                        encrypt = encrypt,
                        compress = compress,
                        freshOnly = freshOnly
                )
            })

    private fun getObservableForOperation(cacheOperation: Operation) =
            VolleyObservable.createDefault(
                    requestQueue,
                    gson,
                    responseClass,
                    rxCache.rxCacheInterceptor.create(
                            CacheInstruction(responseClass, cacheOperation),
                            URL,
                            null
                    ),
                    URL
            )

    override fun getOfflineCompletable(freshOnly: Boolean) =
            getObservableForOperation(Offline(freshOnly))

    override fun getClearEntriesCompletable() =
            getObservableForOperation(Clear()).ignoreElements()!!

    override fun getInvalidateCompletable() =
            getObservableForOperation(Invalidate).ignoreElements()!!

}