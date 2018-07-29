package uk.co.glass_software.android.cache_interceptor.response

data class ResponseWrapper(val responseClass: Class<*>,
                           val response: Any? = null,
                           override var metadata: CacheMetadata? = null) : CacheMetadata.Holder
