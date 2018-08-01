package uk.co.glass_software.android.cache_interceptor.demo.volley

import android.content.Context

import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.facebook.soloader.SoLoader.init

import java.util.concurrent.Callable

import io.reactivex.Observable
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.demo.DemoPresenter
import uk.co.glass_software.android.cache_interceptor.demo.model.JokeResponse
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError

import uk.co.glass_software.android.cache_interceptor.annotations.DEFAULT_DURATION

class VolleyDemoPresenter(context: Context,
                          onLogOutput: (String) -> Unit)
    : DemoPresenter(onLogOutput) {

    private val rxCacheInterceptorFactory: RxCacheInterceptor.Factory<ApiError> = RxCacheInterceptor.buildDefault(context)
    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)

    override fun getResponseObservable(isRefresh: Boolean): Observable<JokeResponse> {
        val cacheOperation = CacheInstruction.Operation.Expiring.Cache()

        val instruction = CacheInstruction(
                JokeResponse::class.java,
                cacheOperation,
                false,
                false
        )

        return VolleyObservable.create(
                requestQueue,
                gson,
                rxCacheInterceptorFactory.create(
                        JokeResponse::class.java,
                        instruction,
                        URL,
                        null
                ),
                URL
        )
    }

    override fun clearEntries() {
//        rxCacheInterceptorFactory.clearOlderEntries()
    }

    companion object {
        private const val URL = DemoPresenter.BASE_URL + DemoPresenter.ENDPOINT
    }

}