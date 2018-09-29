package uk.co.glass_software.android.cache_interceptor.interceptors.error

enum class ErrorCode constructor(val canRetry: Boolean) {
    NETWORK(true),
    UNAUTHORISED(false),
    NOT_FOUND(false),
    UNEXPECTED_RESPONSE(true),
    SERVER_ERROR(true),
    UNKNOWN(true)
}
