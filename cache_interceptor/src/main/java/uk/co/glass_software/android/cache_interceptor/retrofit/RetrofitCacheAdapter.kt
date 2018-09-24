package uk.co.glass_software.android.cache_interceptor.retrofit

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Call
import retrofit2.CallAdapter
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.CLEAR
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.CLEAR_ALL
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptorFactory
import java.lang.reflect.Type

internal class RetrofitCacheAdapter<E>(private val rxCacheFactory: RxCacheInterceptorFactory<E>,
                                       private val instruction: CacheInstruction,
                                       private val rxCallAdapter: CallAdapter<*, *>)
    : CallAdapter<Any, Any>
        where E : Exception,
              E : (E) -> Boolean {
    override fun responseType(): Type = rxCallAdapter.responseType()

    @Suppress("UNCHECKED_CAST")
    override fun adapt(call: Call<Any>): Any {
        return if (instruction.operation.type == CLEAR
                || instruction.operation.type == CLEAR_ALL) {
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
