package uk.co.glass_software.android.cache_interceptor.retrofit

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Call
import retrofit2.CallAdapter
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.*
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import java.lang.reflect.Type

internal class RetrofitCacheAdapter<E>(private val rxCacheFactory: RxCacheInterceptor.Factory<E>,
                                       private val instruction: CacheInstruction,
                                       private val rxCallAdapter: CallAdapter<*, *>)
    : CallAdapter<Any, Any>
        where E : Exception,
              E : NetworkErrorProvider {

    override fun responseType(): Type = rxCallAdapter.responseType()

    @Suppress("UNCHECKED_CAST")
    override fun adapt(call: Call<Any>): Any {
        return if (instruction.operation.type.let {
                    it == CLEAR || it == CLEAR_ALL || it == INVALIDATE
                }) {
            Completable.complete().compose(getRxCacheInterceptor(call))
        } else {
            val responseClass = instruction.responseClass
            val adapted = (rxCallAdapter as CallAdapter<Any, Any>).adapt(call)

            when (adapted) {
                is Observable<*> -> adapted
                        .cast(responseClass)
                        .compose(getRxCacheInterceptor(call))

                is Single<*> -> adapted
                        .cast(responseClass)
                        .compose(getRxCacheInterceptor(call))

                else -> adapted as Any
            }
        }
    }

    private fun getRxCacheInterceptor(call: Call<Any>) =
            rxCacheFactory.create(
                    instruction,
                    call.request().url().toString(),
                    call.request().body()?.toString()
            )

}
