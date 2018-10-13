package uk.co.glass_software.android.cache_interceptor.demo.presenter.retrofit

import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.*
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Expiring.Offline
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse

internal class RetrofitHeaderDemoPresenter(demoActivity: DemoActivity,
                                           uiLogger: Logger)
    : BaseRetrofitDemoPresenter(demoActivity, uiLogger) {

    override fun getResponseObservable(isRefresh: Boolean,
                                       encrypt: Boolean,
                                       compress: Boolean,
                                       freshOnly: Boolean) =
            executeOperation(when {
                isRefresh -> Expiring.Refresh(freshOnly = freshOnly)
                else -> Expiring.Cache(
                        encrypt = encrypt,
                        compress = compress,
                        freshOnly = freshOnly
                )
            })

    override fun getOfflineCompletable(freshOnly: Boolean) =
            executeOperation(Offline(freshOnly))

    override fun getClearEntriesCompletable() =
            executeOperation(Clear())
                    .ignoreElements()!!

    override fun getInvalidateCompletable() =
            executeOperation(Invalidate)
                    .ignoreElements()!!

    private fun executeOperation(cacheOperation: CacheInstruction.Operation) =
            catFactClient.instruct(CacheInstruction(
                    CatFactResponse::class.java,
                    cacheOperation
            ))

}
