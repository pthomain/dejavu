package uk.co.glass_software.android.cache_interceptor.retrofit

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.cache_interceptor.annotations.AnnotationHelper
import uk.co.glass_software.android.cache_interceptor.annotations.AnnotationHelper.RxType.*
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class RetrofitCacheAdapterFactory<E> internal constructor(private val rxJava2CallAdapterFactory: RxJava2CallAdapterFactory,
                                                          private val rxCacheFactory: RxCacheInterceptor.Factory<E>,
                                                          private val annotationHelper: AnnotationHelper<E>)
    : CallAdapter.Factory()
        where E : Exception,
              E : NetworkErrorProvider {

    override fun get(returnType: Type,
                     annotations: Array<Annotation>,
                     retrofit: Retrofit): CallAdapter<*, *> {
        val rawType = getRawType(returnType)
        val callAdapter = rxJava2CallAdapterFactory.get(
                returnType,
                annotations,
                retrofit
        )!!

        val rxType = when (rawType) {
            Single::class.java -> SINGLE
            Observable::class.java -> OBSERVABLE
            Completable::class.java -> COMPLETABLE
            else -> null
        }

        if (rxType != null) {
            val responseClass = when (rxType) {
                OBSERVABLE,
                SINGLE -> {
                    if (returnType is ParameterizedType)
                        getRawType(getParameterUpperBound(0, returnType))
                    else null
                }
                else -> null
            } ?: Any::class.java

            return annotationHelper.process(
                    annotations,
                    rxType,
                    responseClass
            )?.let {
                RetrofitCacheAdapter(
                        rxCacheFactory,
                        it,
                        callAdapter
                )
            } ?: callAdapter
        }

        return callAdapter
    }

}