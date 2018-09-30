package uk.co.glass_software.android.cache_interceptor.demo.presenter.volley

import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import io.reactivex.Completable
import io.reactivex.Observable
import uk.co.glass_software.android.boilerplate.Boilerplate.context
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.demo.DemoActivity
import uk.co.glass_software.android.cache_interceptor.demo.model.CatFactResponse
import uk.co.glass_software.android.cache_interceptor.demo.presenter.BaseDemoPresenter

internal class VolleyDemoPresenter(demoActivity: DemoActivity,
                                   uiLogger: Logger)
    : BaseDemoPresenter(demoActivity) {

    //    private val rxCacheInterceptorFactory: RxCacheInterceptorFactory<ApiError> = RxCacheInterceptorFactory.buildDefault(context)
    private val requestQueue: RequestQueue = Volley.newRequestQueue(context)

    override fun getResponseObservable(isRefresh: Boolean): Observable<CatFactResponse> {
//        val cacheOperation = CacheInstruction.Operation.Expiring.Cache()

//        val instruction = CacheInstruction(
//                CatFactResponse::class.java,
//                cacheOperation,
//                false
//        )

        return Observable.error(NoSuchElementException())
//        return VolleyObservable.create(
//                requestQueue,
//                gson,
//                rxCacheInterceptorFactory.create(
//                        CatFactResponse::class.java,
//                        instruction,
//                        URL,
//                        null
//                ),
//                URL
//        )
    }

    override fun getClearEntriesCompletable(): Completable {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    //    {
//        rxCacheInterceptorFactory.clearOlderEntries()
//    }

    companion object {
        private const val URL = BaseDemoPresenter.BASE_URL + BaseDemoPresenter.ENDPOINT
    }

}