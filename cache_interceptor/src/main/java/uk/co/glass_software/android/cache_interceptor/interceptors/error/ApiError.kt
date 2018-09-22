package uk.co.glass_software.android.cache_interceptor.interceptors.error

import uk.co.glass_software.android.cache_interceptor.interceptors.error.ErrorCode.UNKNOWN

data class ApiError constructor(private val throwable: Throwable,
                                val httpStatus: Int = NON_HTTP_STATUS,
                                val errorCode: ErrorCode = UNKNOWN,
                                val description: String? = null)
    : Exception(throwable),
        (ApiError) -> Boolean {

    val isNetworkError: Boolean
        get() = errorCode === ErrorCode.NETWORK

    override fun invoke(apiError: ApiError) = apiError.isNetworkError

    companion object {
        const val NON_HTTP_STATUS = -1
        fun from(throwable: Throwable) = throwable as? ApiError
    }
}