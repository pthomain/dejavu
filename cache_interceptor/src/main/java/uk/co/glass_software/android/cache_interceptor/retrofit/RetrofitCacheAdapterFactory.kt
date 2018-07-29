package uk.co.glass_software.android.cache_interceptor.retrofit

import android.content.Context
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.cache_interceptor.R
import uk.co.glass_software.android.cache_interceptor.annotations.AnnotationHelper
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class RetrofitCacheAdapterFactory<E> internal constructor(private val rxJava2CallAdapterFactory: RxJava2CallAdapterFactory,
                                                          private val rxCacheFactory: RxCacheInterceptor.Factory<E>,
                                                          private val annotationHelper: AnnotationHelper)
    : CallAdapter.Factory()
        where E : Exception,
              E : (E) -> Boolean {

    override fun get(returnType: Type,
                     annotations: Array<Annotation>,
                     retrofit: Retrofit): CallAdapter<*, *> {
        val rawType = CallAdapter.Factory.getRawType(returnType)
        val wrappedAdapter = getCallAdapter(returnType, annotations, retrofit)
        val isSingle = rawType == Single::class.java

        if ((rawType == Observable::class.java || isSingle) && returnType is ParameterizedType) {
            val observableType = CallAdapter.Factory.getParameterUpperBound(0, returnType)
            val rawObservableType = CallAdapter.Factory.getRawType(observableType)
            val responseClass = CallAdapter.Factory.getRawType(rawObservableType)

            return annotationHelper.process(annotations, isSingle, responseClass)
                    ?.let {
                        create(
                                rxCacheFactory,
                                it,
                                responseClass,
                                wrappedAdapter
                        )
                    } ?: wrappedAdapter
        }

        return wrappedAdapter
    }

    private fun create(rxCacheFactory: RxCacheInterceptor.Factory<E>,
                           instruction: CacheInstruction,
                           responseClass: Class<*>,
                           callAdapter: CallAdapter<*, *>)
            : RetrofitCacheAdapter<E> {
        return RetrofitCacheAdapter(
                rxCacheFactory,
                instruction,
                responseClass,
                callAdapter
        )
    }

    private fun getCallAdapter(returnType: Type,
                               annotations: Array<Annotation>,
                               retrofit: Retrofit): CallAdapter<*, *> = rxJava2CallAdapterFactory.get(
            returnType,
            annotations,
            retrofit
    )

    companion object {

        fun <E> builder(context: Context,
                        errorFactory: (Throwable) -> E)
                : RetrofitCacheAdapterFactoryBuilder<E>
                where E : Exception,
                      E : (E) -> Boolean = RetrofitCacheAdapterFactoryBuilder(
                context,
                errorFactory
        )

        fun buildDefault(context: Context): RetrofitCacheAdapterFactory<ApiError> = RetrofitCacheAdapterFactory(
                RxJava2CallAdapterFactory.create(),
                RxCacheInterceptor.buildDefault(context),
                AnnotationHelper()
        )
    }

}