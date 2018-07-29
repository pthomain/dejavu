package uk.co.glass_software.android.cache_interceptor.demo.volley

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.subjects.PublishSubject
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor

class VolleyObservable<E, R> private constructor(private val requestQueue: RequestQueue,
                                                 private val gson: Gson,
                                                 private val responseClass: Class<R>,
                                                 private val url: String)
    : Observable<R>()
        where E : Exception,
              E : (E) -> Boolean {

    private val publishSubject: PublishSubject<R> = PublishSubject.create()

    override fun subscribeActual(observer: Observer<in R>) {
        publishSubject.subscribe(observer)
        requestQueue.add(StringRequest(
                Request.Method.GET,
                url,
                Response.Listener(this::onResponse),
                Response.ErrorListener(this::onError)
        ))
    }

    private fun onResponse(response: String) {
        publishSubject.onNext(gson.fromJson(response, responseClass))
        publishSubject.onComplete()
    }

    private fun onError(volleyError: VolleyError) {
        publishSubject.onError(volleyError)
    }

    companion object {
        fun <E, R> create(requestQueue: RequestQueue,
                          gson: Gson,
                          cacheInterceptor: RxCacheInterceptor<E, R>,
                          url: String)
                : Observable<R>
                where E : Exception,
                      E : (E) -> Boolean = VolleyObservable<E, R>(
                requestQueue,
                gson,
                cacheInterceptor.responseClass,
                url
        )
                .compose(cacheInterceptor)
                .map { it.response }
    }
}
