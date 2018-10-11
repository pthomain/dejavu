package uk.co.glass_software.android.cache_interceptor.retrofit

import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Call
import retrofit2.CallAdapter
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.RxCache
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Type.*
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import java.lang.reflect.Type

@Suppress("UNCHECKED_CAST")
internal class RetrofitCacheAdapter<E>(private val rxCacheFactory: RxCacheInterceptor.Factory<E>,
                                       private val logger: Logger,
                                       private val gson: Gson,
                                       private val methodDescription: String,
                                       private val instruction: CacheInstruction?,
                                       private val rxCallAdapter: CallAdapter<Any, Any>)
    : CallAdapter<Any, Any>
        where E : Exception,
              E : NetworkErrorProvider {

    override fun responseType(): Type = rxCallAdapter.responseType()

    override fun adapt(call: Call<Any>): Any {
        val header = call.request().header(RxCache.RxCacheHeader)

        return when {
            instruction != null -> if (header != null) adaptedByHeader(call, header, true)
            else adaptedWithInstruction(call, instruction)

            header != null -> adaptedByHeader(call, header, false)

            else -> adaptedByDefaultRxJavaAdapter(call)
        }
    }

    private fun adaptedWithInstruction(call: Call<Any>,
                                       instruction: CacheInstruction): Any {
        logger.d("Found a cache instruction on $methodDescription: $instruction")

        return if (instruction.operation.type.let {
                    it == CLEAR || it == CLEAR_ALL || it == INVALIDATE
                }) {
            Completable.complete().compose(getRxCacheInterceptor(call, instruction))
        } else {
            val responseClass = instruction.responseClass
            val adapted = rxCallAdapter.adapt(call)

            when (adapted) {
                is Observable<*> -> adapted
                        .cast(responseClass)
                        .compose(getRxCacheInterceptor(call, instruction))

                is Single<*> -> adapted
                        .cast(responseClass)
                        .compose(getRxCacheInterceptor(call, instruction))

                else -> adapted as Any
            }
        }
    }

    private fun adaptedByHeader(call: Call<Any>,
                                header: String,
                                isOverridingAnnotation: Boolean): Any {
        if (isOverridingAnnotation) {
            logger.d("WARNING: $methodDescription contains a cache instruction BOTH by annotation and by header."
                    + " The header instruction will take precedence."
            )
        }

        return try {
            adaptedWithInstruction(
                    call,
                    gson.fromJson(header, CacheInstruction::class.java)
            )
        } catch (e: Exception) {
            logger.e("Found a header cache instruction on $methodDescription but it could not be deserialised."
                    + " This call won't be cached.")
            adaptedByDefaultRxJavaAdapter(call)
        }
    }

    private fun adaptedByDefaultRxJavaAdapter(call: Call<Any>): Any {
        logger.d("No annotation or header cache instruction found for $methodDescription,"
                + " the call will be adapted with the default RxJava 2 adapter."
        )
        return rxCallAdapter.adapt(call)
    }

    private fun getRxCacheInterceptor(call: Call<Any>,
                                      instruction: CacheInstruction) =
            rxCacheFactory.create(
                    instruction,
                    call.request().url().toString(),
                    call.request().body()?.toString()
            )

}
