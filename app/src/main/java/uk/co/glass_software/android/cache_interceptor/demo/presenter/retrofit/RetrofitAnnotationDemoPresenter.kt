package uk.co.glass_software.android.cache_interceptor.demo.presenter.retrofit

import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity

internal class RetrofitAnnotationDemoPresenter(demoActivity: DemoActivity,
                                               uiLogger: Logger)
    : BaseRetrofitDemoPresenter(demoActivity, uiLogger) {

    override fun getResponseObservable(isRefresh: Boolean,
                                       encrypt: Boolean,
                                       compress: Boolean,
                                       freshOnly: Boolean) =
            if (isRefresh) {
                when {
                    freshOnly -> catFactClient.refreshFreshOnly()
                    else -> catFactClient.refresh()
                }
            } else {
                when {
                    freshOnly && compress && encrypt -> catFactClient.freshOnlyCompressedEncrypted()
                    freshOnly && compress -> catFactClient.freshOnlyCompressed()
                    freshOnly && encrypt -> catFactClient.freshOnlyEncrypted()
                    freshOnly -> catFactClient.freshOnly()
                    compress && encrypt -> catFactClient.compressedEncrypted()
                    compress -> catFactClient.compressed()
                    encrypt -> catFactClient.encrypted()
                    else -> catFactClient.get()
                }
            }

    override fun getOfflineCompletable(freshOnly: Boolean) =
            if (freshOnly) catFactClient.offlineFreshOnly()
            else catFactClient.offline()

    override fun getClearEntriesCompletable() = catFactClient.clearCache()

    override fun getInvalidateCompletable() = catFactClient.invalidate()

}
