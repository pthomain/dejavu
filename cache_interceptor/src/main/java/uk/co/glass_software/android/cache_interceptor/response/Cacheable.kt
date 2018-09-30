package uk.co.glass_software.android.cache_interceptor.response

import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError

/**
 * Default implementation of CacheMetadata.Holder. Have the response extend this class
 * if you want to inherit from the default metadata holding and error handling mechanisms.
 * Alternatively, if your response class cannot extend this class, have it implement the
 * CacheMetadata.Holder interface in a similar fashion as this class' implementation.
 * To provide your own error handling via an error factory, see ApiErrorFactory.
 */
abstract class Cacheable : CacheMetadata.Holder<ApiError>{

    override var metadata: CacheMetadata<ApiError>? = null

}