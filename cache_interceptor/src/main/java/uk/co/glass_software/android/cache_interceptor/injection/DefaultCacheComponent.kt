package uk.co.glass_software.android.cache_interceptor.injection

import dagger.Component
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError

@Component(modules = [DefaultConfigurationModule::class])
internal interface DefaultCacheComponent : CacheComponent<ApiError>
