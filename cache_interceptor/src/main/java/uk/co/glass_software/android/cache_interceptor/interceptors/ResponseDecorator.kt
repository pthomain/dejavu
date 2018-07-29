package uk.co.glass_software.android.cache_interceptor.interceptors

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.Single
import io.reactivex.SingleTransformer
import uk.co.glass_software.android.cache_interceptor.response.ResponseWrapper

class ResponseDecorator : ObservableTransformer<ResponseWrapper, Any>,
        SingleTransformer<ResponseWrapper, Any> {

    override fun apply(upstream: Observable<ResponseWrapper>) = upstream.flatMap(this::decorate)!!

    override fun apply(upstream: Single<ResponseWrapper>) = upstream.flatMapObservable(this::decorate).firstOrError()!!

    private fun decorate(wrapper: ResponseWrapper): Observable<Any> {
        return if (wrapper.response != null) {
            Observable.just(wrapper.response)
        } else {
            Observable.error(NoSuchElementException("No response found"))// TODO check merging situation
        }
    }
}
