package uk.co.glass_software.android.cache_interceptor.demo.retrofit

import android.content.Context
import com.facebook.soloader.SoLoader.init

import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uk.co.glass_software.android.cache_interceptor.demo.DemoPresenter
import uk.co.glass_software.android.cache_interceptor.demo.model.JokeResponse
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiErrorFactory
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory
import uk.co.glass_software.android.shared_preferences.utils.Logger

class RetrofitDemoPresenter(context: Context,
                            onLogOutput: (String) -> Unit)
    : DemoPresenter(onLogOutput) {

    private val jokeClient: JokeClient
    private val adapterFactory: RetrofitCacheAdapterFactory<*>

    init {
        adapterFactory = RetrofitCacheAdapterFactory.builder(context, ApiErrorFactory())
                .logger(simpleLogger)
                .build()

        val retrofit = Retrofit.Builder()
                .baseUrl(DemoPresenter.BASE_URL)
                .client(getOkHttpClient(simpleLogger))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(adapterFactory)
                .build()

        jokeClient = retrofit.create(JokeClient::class.java)
    }

    private fun getOkHttpClient(logger: Logger): OkHttpClient {
        val httpClientBuilder = OkHttpClient.Builder()
        httpClientBuilder.addInterceptor(getHttpLoggingInterceptor(logger))
        httpClientBuilder.followRedirects(true)
        return httpClientBuilder.build()
    }

    private fun getHttpLoggingInterceptor(logger: Logger): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor { logger.d(this, it) }
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return interceptor
    }

    override fun getResponseObservable(isRefresh: Boolean): Observable<out JokeResponse> {
        return if (isRefresh) jokeClient.refresh() else jokeClient.get()
    }

    override fun clearEntries() {
//        adapterFactory.clearOlderEntries()
    }

}
