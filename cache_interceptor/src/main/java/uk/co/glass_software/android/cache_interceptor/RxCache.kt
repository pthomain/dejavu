package uk.co.glass_software.android.cache_interceptor

import uk.co.glass_software.android.cache_interceptor.configuration.CacheConfiguration
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.injection.CacheComponent
import uk.co.glass_software.android.cache_interceptor.injection.DaggerDefaultCacheComponent
import uk.co.glass_software.android.cache_interceptor.injection.DefaultConfigurationModule
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiErrorFactory
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory

open class RxCache<E> internal constructor(component: CacheComponent<E>)
        where E : Exception,
              E : NetworkErrorProvider {

    val configuration: CacheConfiguration<E> = component.configuration()
    val retrofitCacheAdapterFactory: RetrofitCacheAdapterFactory<E> = component.retrofitCacheAdapterFactory()
    val rxCacheInterceptor: RxCacheInterceptor.Factory<E> = component.rxCacheInterceptorFactory()

    companion object {

        private fun defaultComponentProvider() = { cacheConfiguration: CacheConfiguration<ApiError> ->
            DaggerDefaultCacheComponent
                    .builder()
                    .defaultConfigurationModule(DefaultConfigurationModule(cacheConfiguration))
                    .build()
        }

        fun builder() = CacheConfiguration.builder(
                ApiErrorFactory(),
                defaultComponentProvider()
        )
    }
}
