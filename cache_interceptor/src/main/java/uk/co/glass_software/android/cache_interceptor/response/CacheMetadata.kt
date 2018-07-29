package uk.co.glass_software.android.cache_interceptor.response

import uk.co.glass_software.android.cache_interceptor.interceptors.cache.CacheToken

data class CacheMetadata<E>(val cacheToken: CacheToken? = null,
                            val exception: E? = null)
        where E : Exception,
              E : (E) -> Boolean {

    interface Holder<E>
            where E : Exception,
                  E : (E) -> Boolean {
        var metadata: CacheMetadata<E>?
    }

}