package uk.co.glass_software.android.cache_interceptor.interceptors

import android.content.Context
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.SingleTransformer
import net.bytebuddy.ByteBuddy
import net.bytebuddy.android.AndroidClassLoadingStrategy
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper
import java.lang.reflect.Modifier
import uk.co.glass_software.android.shared_preferences.utils.Logger

@Suppress("UNCHECKED_CAST")
internal class ResponseDecorator<E>(context: Context,
                                    private val logger: Logger)
    : ObservableTransformer<ResponseWrapper<E>, Any>,
        SingleTransformer<ResponseWrapper<E>, Any>
        where E : Exception,
              E : (E) -> Boolean {

    private val loadingStrategy = AndroidClassLoadingStrategy.Injecting(context.filesDir)

    override fun apply(upstream: Observable<ResponseWrapper<E>>) = upstream.flatMap(this::decorate)!!

    override fun apply(upstream: Single<ResponseWrapper<E>>) = upstream.flatMapObservable(this::decorate).firstOrError()!!

    private fun decorate(wrapper: ResponseWrapper<E>): Observable<Any> {
        val metadataHolderClass = object : TypeToken<CacheMetadata.Holder<E>>() {}.rawType
        val response = wrapper.response
        val responseClass = wrapper.responseClass
        val metadata = wrapper.metadata

        return if (response != null && metadata != null) {
            addMetadata(
                    metadataHolderClass,
                    response,
                    responseClass,
                    metadata
            ) as Observable<Any>
        } else {
            // TODO check merging situation
            val exception = metadata?.exception ?: NoSuchElementException("No response found")
            Observable.error(exception)
        }
    }

    private fun addMetadata(holderClass: Class<in CacheMetadata.Holder<E>>,
                            response: Any,
                            responseClass: Class<*>,
                            metadata: CacheMetadata<E>) =
            if (holderClass.isAssignableFrom(responseClass)) {
                val holder = response as CacheMetadata.Holder<E>
                holder.metadata = metadata
                Observable.just(holder)
            } else {
                subtypeForMetadata(
                        holderClass,
                        response,
                        responseClass,
                        metadata
                )
            }

    private fun subtypeForMetadata(holderClass: Class<in CacheMetadata.Holder<E>>,
                                   response: Any,
                                   responseClass: Class<*>,
                                   metadata: CacheMetadata<E>): Observable<Any> {
        if (Modifier.isFinal(responseClass.modifiers)) {
            logger.e(
                    this,
                    "Could not subtype final class '${responseClass.simpleName}' to bind cache metadata." +
                            " If you want to enable metadata for this class make it non-final" +
                            " or have it extend the 'CacheMetadata.Holder' interface." +
                            " The 'mergeOnNextOnError' directive will be ignored for classes" +
                            " that do not support cache metadata." //TODO
            )
            return Observable.just(response)
        }

        val extendedHolder = ByteBuddy()
                .rebase(responseClass)
                .implement(holderClass)
                .name("RxCache${responseClass.simpleName}")
                .make()
                .load(javaClass.classLoader, loadingStrategy)
                .loaded
                .newInstance() as CacheMetadata.Holder<E>

        extendedHolder.metadata = metadata

        return Observable.just(extendedHolder)
    }

}
