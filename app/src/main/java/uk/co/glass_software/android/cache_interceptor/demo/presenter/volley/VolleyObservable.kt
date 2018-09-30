package uk.co.glass_software.android.cache_interceptor.demo.presenter.volley

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.subjects.PublishSubject
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheTransformer
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError

class VolleyObservable<E, R> private constructor(private val requestQueue: RequestQueue,
                                                 private val gson: Gson,
                                                 private val responseClass: Class<R>,
                                                 private val url: String)
    : Observable<R>()
        where E : Exception,
              E : NetworkErrorProvider {

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
                          responseClass: Class<R>,
                          cacheInterceptor: RxCacheTransformer,
                          url: String) where E : Exception,
                                             E : NetworkErrorProvider =
                VolleyObservable<E, R>(
                        requestQueue,
                        gson,
                        responseClass,
                        url
                ).cast(Any::class.java)
                        .compose(cacheInterceptor)
                        .cast(responseClass)!!

        fun <R> createDefault(requestQueue: RequestQueue,
                              gson: Gson,
                              responseClass: Class<R>,
                              cacheInterceptor: RxCacheTransformer,
                              url: String) =
                create<ApiError, R>(
                        requestQueue,
                        gson,
                        responseClass,
                        cacheInterceptor,
                        url
                )
    }
}
