package uk.co.glass_software.android.cache_interceptor.retrofit

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.SingleTransformer
import retrofit2.Call
import retrofit2.CallAdapter
import uk.co.glass_software.android.cache_interceptor.R
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.interceptors.ResponseDecorator
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import java.lang.reflect.Type

internal class RetrofitCacheAdapter<E>(private val rxCacheFactory: RxCacheInterceptor.Factory<E>,
                                       private val instruction: CacheInstruction,
                                       private val responseClass: Class<*>,
                                       private val rxCallAdapter: CallAdapter<*, *>)
    : CallAdapter<Any, Any>
        where E : Exception,
              E : (E) -> Boolean {
    override fun responseType(): Type = rxCallAdapter.responseType()

    @Suppress("UNCHECKED_CAST")
    override fun adapt(call: Call<Any>): Any {
        val adapted = (rxCallAdapter as CallAdapter<Any, Any>).adapt(call)
        val body = call.request().body()

        val rxCacheInterceptor = rxCacheFactory.create(
                responseClass,
                instruction,
                call.request().url().toString(),
                body?.toString()
        )

        return when (adapted) {
            is Observable<*> -> adapted
                    .cast(responseClass)
                    .compose(rxCacheInterceptor)

            is Single<*> -> adapted
                    .cast(responseClass)
                    .compose(rxCacheInterceptor)

            else -> adapted as Any
        }
    }

}
