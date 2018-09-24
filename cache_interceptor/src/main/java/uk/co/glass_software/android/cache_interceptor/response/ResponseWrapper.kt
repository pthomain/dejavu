package uk.co.glass_software.android.cache_interceptor.response

internal data class ResponseWrapper<E>(val responseClass: Class<*>,
                                       val response: Any? = null,
                                       override var metadata: CacheMetadata<E>?)
    : CacheMetadata.Holder<E>
        where E : Exception,
              E : (E) -> Boolean
