package uk.co.glass_software.android.cache_interceptor.response

import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.token.CacheToken

data class CacheMetadata<E>(@Transient val cacheToken: CacheToken,
                            @Transient val exception: E? = null,
                            @Transient val callDuration: Long = 0L)
        where E : Exception,
              E : NetworkErrorProvider {

    interface Holder<E>
            where E : Exception,
                  E : NetworkErrorProvider {
        var metadata: CacheMetadata<E>?
    }

}