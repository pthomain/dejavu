package uk.co.glass_software.android.cache_interceptor.configuration

interface ErrorFactory<E> {
    fun getError(throwable: Throwable): E
}