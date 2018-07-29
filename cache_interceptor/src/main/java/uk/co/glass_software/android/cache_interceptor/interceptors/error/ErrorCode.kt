package uk.co.glass_software.android.cache_interceptor.interceptors.error

enum class ErrorCode constructor(val isRecoverable: Boolean) {
    NETWORK(true),
    UNAUTHORISED(false),
    NOT_FOUND(false),
    UNEXPECTED_RESPONSE(true),
    UNKNOWN(true)
}
