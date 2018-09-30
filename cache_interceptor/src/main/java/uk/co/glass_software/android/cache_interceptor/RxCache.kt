package uk.co.glass_software.android.cache_interceptor

import android.content.Context
import uk.co.glass_software.android.cache_interceptor.configuration.CacheConfiguration
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.injection.CacheComponent
import uk.co.glass_software.android.cache_interceptor.injection.DaggerDefaultCacheComponent
import uk.co.glass_software.android.cache_interceptor.injection.DefaultConfigurationModule
import uk.co.glass_software.android.cache_interceptor.interceptors.RxCacheInterceptor
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError
import uk.co.glass_software.android.cache_interceptor.retrofit.RetrofitCacheAdapterFactory

sealed class RxCache<E>(private val cacheConfiguration: CacheConfiguration<E>,
                        component: CacheComponent<E>)
        where E : Exception,
              E : NetworkErrorProvider {

    class Default(cacheConfiguration: CacheConfiguration<ApiError>)
        : RxCache<ApiError>(
            cacheConfiguration,
            DaggerDefaultCacheComponent
                    .builder()
                    .defaultConfigurationModule(DefaultConfigurationModule(cacheConfiguration))
                    .build()
    )

    val retrofitCacheAdapterFactory: RetrofitCacheAdapterFactory<E> = component.retrofitCacheAdapterFactory()
    val rxCacheAdapterFactory: RxCacheInterceptor.Factory<E> = component.rxCacheInterceptorFactory()

    companion object {

        fun build(context: Context) =
                build(CacheConfiguration.builder().build(context))

        fun build(cacheConfiguration: CacheConfiguration<ApiError>) =
                Default(cacheConfiguration)

    }
}
