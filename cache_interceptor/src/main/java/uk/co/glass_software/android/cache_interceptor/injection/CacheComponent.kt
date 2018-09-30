package uk.co.glass_software.android.cache_interceptor.injection

import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory

internal interface CacheComponent<E>
        where E : Exception,
              E : NetworkErrorProvider {

    fun rxCacheInterceptorFactory(): RxCacheInterceptor.Factory<E>

    fun retrofitCacheAdapterFactory(): RetrofitCacheAdapterFactory<E>

}