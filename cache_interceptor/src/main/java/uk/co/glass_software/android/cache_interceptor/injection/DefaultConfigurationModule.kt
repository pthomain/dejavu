package uk.co.glass_software.android.cache_interceptor.injection

import dagger.Module
import uk.co.glass_software.android.cache_interceptor.configuration.CacheConfiguration
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError

@Module
internal class DefaultConfigurationModule(configuration: CacheConfiguration<ApiError>)
    : ConfigurationModule<ApiError>(configuration)
