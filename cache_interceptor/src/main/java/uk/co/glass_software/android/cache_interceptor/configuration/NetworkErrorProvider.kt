package uk.co.glass_software.android.cache_interceptor.configuration

interface NetworkErrorProvider {
    fun isNetworkError(): Boolean
}