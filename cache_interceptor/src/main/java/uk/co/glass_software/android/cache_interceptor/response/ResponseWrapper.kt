package uk.co.glass_software.android.cache_interceptor.response

import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider

internal data class ResponseWrapper<E>(val responseClass: Class<*>,
                                       val response: Any?,
                                       val start: Long,
                                       override var metadata: CacheMetadata<E>)
    : CacheMetadata.Holder<E>
        where E : Exception,
              E : NetworkErrorProvider
