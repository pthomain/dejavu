package uk.co.glass_software.android.cache_interceptor.demo.retrofit

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uk.co.glass_software.android.boilerplate.Boilerplate.context
import uk.co.glass_software.android.boilerplate.lambda.Callback1
import uk.co.glass_software.android.boilerplate.log.Logger
import uk.co.glass_software.android.cache_interceptor.demo.DemoPresenter
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiErrorFactory
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory

class RetrofitDemoPresenter(context: Context,
                            onLogOutput: (String) -> Unit)
    : DemoPresenter(context, onLogOutput) {

    private val adapterFactory = RetrofitCacheAdapterFactory
            .builder(context, ApiErrorFactory())
            .timeOutInSeconds(5)
            .logger(uiLogger)
            .build()

    private val retrofit = Retrofit.Builder()
            .baseUrl(DemoPresenter.BASE_URL)
            .client(getOkHttpClient(uiLogger))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(adapterFactory)
            .build()

    private val jokeClient = retrofit.create(JokeClient::class.java)

    private fun getOkHttpClient(logger: Logger) = OkHttpClient.Builder().let {
        it.addInterceptor(getHttpLoggingInterceptor(logger))
        it.followRedirects(true)
        it.build()
    }

    private fun getHttpLoggingInterceptor(logger: Logger) =
            HttpLoggingInterceptor { logger.d(it) }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

    override fun getResponseObservable(isRefresh: Boolean) =
            (if (isRefresh) jokeClient.refresh() else jokeClient.get())!!

    override fun clearEntries() {
        jokeClient.clearCache()
    }

}
