package uk.co.glass_software.android.cache_interceptor.demo.model

import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata

class JokeResponse : CacheMetadata.Holder<ApiError> {

    var value: Value? = null

    override var metadata: CacheMetadata<ApiError>? = null

    class Value {
        var joke: String? = null
    }
}