package uk.co.glass_software.android.cache_interceptor.interceptors

import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.SingleTransformer
import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import uk.co.glass_software.android.cache_interceptor.interceptors.ResponseDecorator.Companion.propertyName
import uk.co.glass_software.android.cache_interceptor.interceptors.ResponseDecorator.Companion.propertySetter
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper

internal class ResponseDecorator<E>
    : ObservableTransformer<ResponseWrapper<E>, Any>,
        SingleTransformer<ResponseWrapper<E>, Any>
        where E : Exception,
              E : (E) -> Boolean {

    private val byteBuddy = ByteBuddy()

    companion object {
        const val propertyName = "rxCacheInterceptorCacheMetadata"
        const val propertySetter = "setRxCacheInterceptorCacheMetadata"
        const val propertyGetter = "getRxCacheInterceptorCacheMetadata"
    }

    override fun apply(upstream: Observable<ResponseWrapper<E>>) = upstream.flatMap(this::decorate)!!

    override fun apply(upstream: Single<ResponseWrapper<E>>) = upstream.flatMapObservable(this::decorate).firstOrError()!!

    private fun decorate(wrapper: ResponseWrapper<E>) = if (wrapper.response != null) {
        checkInterface(wrapper)
    } else {
        // TODO check merging situation
        val exception = wrapper.metadata?.exception ?: NoSuchElementException("No response found")
        Observable.error(exception)
    }

    @Suppress("UNCHECKED_CAST")
    private fun checkInterface(wrapper: ResponseWrapper<E>) : Observable<Any>{
        val metadataType = object : TypeToken<CacheMetadata.Holder<E>>() {}.rawType
        val responseClass = wrapper.responseClass
        if(metadataType.isAssignableFrom(responseClass)){
            val holder = wrapper.response as CacheMetadata.Holder<E>
            holder.metadata = wrapper.metadata
            return Observable.just(holder)
        }
        return Observable.just(wrapper.response)
    }

    private fun <E> subtypeForMetadata(responseWrapper: ResponseWrapper<E>): Observable<Any>
            where E : Exception,
                  E : (E) -> Boolean {
        try {
            val metadataType = object : TypeToken<CacheMetadata<E>>() {}.rawType
            val responseClass = responseWrapper.responseClass

            val subtype = byteBuddy
                    .subclass(responseClass)
                    .defineProperty(propertyName, metadataType)
                    .make()
                    .load(this::class.java.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                    .loaded

            val setter = subtype.getDeclaredMethod(propertySetter, metadataType)
            setter.invoke(subtype, responseWrapper.metadata)

            return Observable.just(subtype)
        } catch (e: Exception) {
            return Observable.just(responseWrapper)
        }
    }

}
