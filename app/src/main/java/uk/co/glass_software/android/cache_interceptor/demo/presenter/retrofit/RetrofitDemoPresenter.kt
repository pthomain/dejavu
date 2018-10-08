package uk.co.glass_software.android.cache_interceptor.demo.presenter.retrofit

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity
import uk.co.glass_software.android.cache_interceptor.demo.presenter.BaseDemoPresenter

internal class RetrofitDemoPresenter(demoActivity: DemoActivity,
                                     uiLogger: Logger)
    : BaseDemoPresenter(demoActivity, uiLogger) {

    private val retrofit = Retrofit.Builder()
            .baseUrl(BaseDemoPresenter.BASE_URL)
            .client(getOkHttpClient(uiLogger))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(rxCache.retrofitCacheAdapterFactory)
            .build()

    private val catFactClient = retrofit.create(CatFactClient::class.java)

    private fun getOkHttpClient(logger: Logger) = OkHttpClient.Builder().let {
        it.addInterceptor(getHttpLoggingInterceptor(logger))
        it.followRedirects(true)
        it.build()
    }

    private fun getHttpLoggingInterceptor(logger: Logger) =
            HttpLoggingInterceptor { logger.d(it) }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

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
