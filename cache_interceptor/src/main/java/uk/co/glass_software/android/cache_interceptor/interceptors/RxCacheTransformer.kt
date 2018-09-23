package uk.co.glass_software.android.cache_interceptor.interceptors

import io.reactivex.CompletableTransformer
import io.reactivex.ObservableTransformer
import io.reactivex.SingleTransformer

interface RxCacheTransformer
    : ObservableTransformer<Any, Any>,
        SingleTransformer<Any, Any>,
        CompletableTransformer
