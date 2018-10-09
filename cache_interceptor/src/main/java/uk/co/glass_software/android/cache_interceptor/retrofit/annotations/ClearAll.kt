package uk.co.glass_software.android.cache_interceptor.retrofit.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ClearAll(val clearOldEntriesOnly: Boolean = false)

