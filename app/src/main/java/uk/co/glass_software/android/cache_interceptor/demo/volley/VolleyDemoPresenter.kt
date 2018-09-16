package uk.co.glass_software.android.cache_interceptor.demo.volley

import android.content.Context

import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

import io.reactivex.Observable
import uk.co.glass_software.android.boilerplate.lambda.Callback1
import uk.co.glass_software.android.cache_interceptor.demo.DemoPresenter
import uk.co.glass_software.android.cache_interceptor.demo.model.JokeResponse

class VolleyDemoPresenter(context: Context,
                          onLogOutput: (String) -> Unit)
    : DemoPresenter(context, onLogOutput) {

    //    private val rxCacheInterceptorFactory: RxCacheInterceptorFactory<ApiError> = RxCacheInterceptorFactory.buildDefault(context)
    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)

    override fun getResponseObservable(isRefresh: Boolean): Observable<JokeResponse> {
//        val cacheOperation = CacheInstruction.Operation.Expiring.Cache()

//        val instruction = CacheInstruction(
//                JokeResponse::class.java,
//                cacheOperation,
//                false
//        )

        return Observable.error(NoSuchElementException())
//        return VolleyObservable.create(
//                requestQueue,
//                gson,
//                rxCacheInterceptorFactory.create(
//                        JokeResponse::class.java,
//                        instruction,
//                        URL,
//                        null
//                ),
//                URL
//        )
    }

    override fun clearEntries() {
//        rxCacheInterceptorFactory.clearOlderEntries()
    }

    companion object {
        private const val URL = DemoPresenter.BASE_URL + DemoPresenter.ENDPOINT
    }

}