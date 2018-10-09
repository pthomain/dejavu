package uk.co.glass_software.android.cache_interceptor.retrofit

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.AnnotationProcessor
import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.AnnotationProcessor.RxType.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class RetrofitCacheAdapterFactory<E> internal constructor(private val rxJava2CallAdapterFactory: RxJava2CallAdapterFactory,
                                                          private val rxCacheFactory: RxCacheInterceptor.Factory<E>,
                                                          private val annotationProcessor: AnnotationProcessor<E>,
                                                          private val logger: Logger)
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

        when (rawType) {
            Single::class.java -> SINGLE
            Observable::class.java -> OBSERVABLE
            Completable::class.java -> COMPLETABLE
            else -> null
        }?.let { rxType ->
            val responseClass = when (rxType) {
                OBSERVABLE,
                SINGLE -> {
                    if (returnType is ParameterizedType)
                        getRawType(getParameterUpperBound(0, returnType))
                    else null
                }
                else -> null
            } ?: Any::class.java

            logger.d("Processing annotation for method returning " + rxType.getTypedName(responseClass))

            return annotationProcessor.process(
                    annotations,
                    rxType,
                    responseClass
            )?.let { instruction ->
                logger.d("Annotation processor for method returning "
                        + rxType.getTypedName(responseClass)
                        + " returned the following instruction "
                        + instruction
                )

                RetrofitCacheAdapter(
                        rxCacheFactory,
                        instruction,
                        callAdapter
                )
            } ?: callAdapter.also {
                logger.d(
                        "Annotation processor did not return any instruction for call returning "
                                + rxType.getTypedName(responseClass)
                )
            }
        }

        return callAdapter.also {
            logger.d("Annotation processor did not return any instruction for call returning $returnType")
        }
    }

}