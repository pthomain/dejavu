package uk.co.glass_software.android.cache_interceptor.retrofit

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.cache_interceptor.annotations.AnnotationHelper
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
        val rawType = CallAdapter.Factory.getRawType(returnType)
        val callAdapter = getCallAdapter(returnType, annotations, retrofit)
        val isSingle = rawType == Single::class.java

        if ((rawType == Observable::class.java || isSingle) && returnType is ParameterizedType) {
            val observableType = CallAdapter.Factory.getParameterUpperBound(0, returnType)
            val responseClass = CallAdapter.Factory.getRawType(observableType)

            return annotationHelper.process(annotations, isSingle, responseClass)
                    ?.let {
                        create(
                                rxCacheFactory,
                                it,
                                responseClass,
                                callAdapter
                        )
                    } ?: callAdapter
        }

        return callAdapter
    }

    private fun create(rxCacheFactory: RxCacheInterceptorFactory<E>,
                       instruction: CacheInstruction,
                       responseClass: Class<*>,
                       callAdapter: CallAdapter<*, *>) = RetrofitCacheAdapter(
            rxCacheFactory,
            instruction,
            responseClass,
            callAdapter
    )

    private fun getCallAdapter(returnType: Type,
                               annotations: Array<Annotation>,
                               retrofit: Retrofit): CallAdapter<*, *> = rxJava2CallAdapterFactory.get(
            returnType,
            annotations,
            retrofit
    )!!

    companion object {

        fun <E> builder(context: Context,
                        errorFactory: (Throwable) -> E)
                : RetrofitCacheAdapterFactoryBuilder<E>
                where E : Exception,
                      E : (E) -> Boolean = RetrofitCacheAdapterFactoryBuilder(
                context,
                errorFactory
        )

        fun buildDefault(context: Context) = RetrofitCacheAdapterFactory(
                RxJava2CallAdapterFactory.create(),
                RxCacheInterceptor.buildDefault(context),
                AnnotationHelper()
        )
    }

}