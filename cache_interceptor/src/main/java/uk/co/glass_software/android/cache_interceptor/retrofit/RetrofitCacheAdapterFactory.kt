package uk.co.glass_software.android.cache_interceptor.retrofit

import android.content.Context
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.cache_interceptor.annotations.AnnotationHelper
import uk.co.glass_software.android.cache_interceptor.annotations.AnnotationHelper.RxType.*
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptorFactory
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class RetrofitCacheAdapterFactory<E> internal constructor(private val rxJava2CallAdapterFactory: RxJava2CallAdapterFactory,
                                                          private val rxCacheFactory: RxCacheInterceptorFactory<E>,
                                                          private val annotationHelper: AnnotationHelper)
    : CallAdapter.Factory()
        where E : Exception,
              E : (E) -> Boolean {

    override fun get(returnType: Type,
                     annotations: Array<Annotation>,
                     retrofit: Retrofit): CallAdapter<*, *> {
        val rawType = getRawType(returnType)
        val callAdapter = getCallAdapter(returnType, annotations, retrofit)

        val rxType = when {
            rawType == Single::class.java -> SINGLE
            rawType == Observable::class.java -> OBSERVABLE
            rawType == Completable::class.java -> COMPLETABLE
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
                create(
                        rxCacheFactory,
                        it,
                        callAdapter
                )
            } ?: callAdapter
        }

        return callAdapter
    }

    private fun create(rxCacheFactory: RxCacheInterceptorFactory<E>,
                       instruction: CacheInstruction,
                       callAdapter: CallAdapter<*, *>) =
            RetrofitCacheAdapter(
                    rxCacheFactory,
                    instruction,
                    callAdapter
            )

    private fun getCallAdapter(returnType: Type,
                               annotations: Array<Annotation>,
                               retrofit: Retrofit): CallAdapter<*, *> =
            rxJava2CallAdapterFactory.get(
                    returnType,
                    annotations,
                    retrofit
            )!!

    companion object {

        fun <E> builder(context: Context,
                        errorFactory: (Throwable) -> E)
                : RetrofitCacheAdapterFactoryBuilder<E>
                where E : Exception,
                      E : (E) -> Boolean =
                RetrofitCacheAdapterFactoryBuilder(
                        context,
                        errorFactory
                )

        fun buildDefault(context: Context) =
                RetrofitCacheAdapterFactory(
                        RxJava2CallAdapterFactory.create(),
                        RxCacheInterceptor.buildDefault(context),
                        AnnotationHelper()
                )
    }

}