package uk.co.glass_software.android.cache_interceptor.demo.model

import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata

data class JokeResponse(var type: String? = null,
                        var value: Value? = null,
                        override var metadata: CacheMetadata<ApiError>? = null) : CacheMetadata.Holder<ApiError>

data class Value(var joke: String? = null)
