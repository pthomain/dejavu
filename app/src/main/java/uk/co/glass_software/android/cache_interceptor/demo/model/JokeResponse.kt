package uk.co.glass_software.android.cache_interceptor.demo.model

import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata

class JokeResponse : CacheMetadata.Holder<ApiError> {

    override var metadata: CacheMetadata<ApiError>? = null

    var type: String? = null
    var value: Value? = null

    inner class Value {
        var joke: String? = null
    }
}