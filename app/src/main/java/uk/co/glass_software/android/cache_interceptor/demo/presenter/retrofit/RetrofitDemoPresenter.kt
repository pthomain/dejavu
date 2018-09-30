package uk.co.glass_software.android.cache_interceptor.demo.presenter.retrofit

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.RxCache
import uk.co.glass_software.android.cache_interceptor.configuration.CacheConfiguration
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity
import uk.co.glass_software.android.cache_interceptor.demo.presenter.BaseDemoPresenter

internal class RetrofitDemoPresenter(demoActivity: DemoActivity,
                                     uiLogger: Logger)
    : BaseDemoPresenter(demoActivity) {

    private val rxCache = RxCache.build(
            CacheConfiguration
                    .builder()
                    .mergeOnNextOnError(true)
                    .networkTimeOutInSeconds(10)
                    .logger(uiLogger)
                    .build(demoActivity)
    )

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

    override fun getResponseObservable(isRefresh: Boolean) = (
            if (isRefresh) catFactClient.refresh()
            else catFactClient.get()
            )!!

    override fun getClearEntriesCompletable() =
            catFactClient.clearCache()!!

}
