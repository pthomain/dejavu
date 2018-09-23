package uk.co.glass_software.android.cache_interceptor.demo.model

import uk.co.glass_software.android.cache_interceptor.interceptors.error.ApiError
import uk.co.glass_software.android.cache_interceptor.response.CacheMetadata

class CatFactResponse : CacheMetadata.Holder<ApiError> {

    var fact: String? = null

    override var metadata: CacheMetadata<ApiError>? = null

}