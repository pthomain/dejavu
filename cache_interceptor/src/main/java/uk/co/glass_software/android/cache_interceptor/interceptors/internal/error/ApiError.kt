package uk.co.glass_software.android.cache_interceptor.interceptors.internal.error

import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ErrorCode.UNKNOWN

data class ApiError constructor(private val throwable: Throwable,
                                val httpStatus: Int = NON_HTTP_STATUS,
                                val errorCode: ErrorCode = UNKNOWN,
                                val description: String? = null)
    : Exception(throwable),
        NetworkErrorProvider {

    override fun isNetworkError() = errorCode === ErrorCode.NETWORK

    companion object {
        const val NON_HTTP_STATUS = -1
        fun from(throwable: Throwable) = throwable as? ApiError
    }
}