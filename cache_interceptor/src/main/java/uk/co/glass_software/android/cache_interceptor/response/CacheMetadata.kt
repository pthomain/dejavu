package uk.co.glass_software.android.cache_interceptor.response

import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken

data class CacheMetadata(var instruction: CacheInstruction,
                         var cacheToken: CacheToken? = null,
                         var exception: Throwable? = null) {
    interface Holder {
        var metadata: CacheMetadata?
    }
}