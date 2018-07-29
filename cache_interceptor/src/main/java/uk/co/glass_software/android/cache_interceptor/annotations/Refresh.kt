package uk.co.glass_software.android.cache_interceptor.annotations


@Target(AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Refresh(val freshOnly: Boolean = false,
                         val mergeOnNextOnError: Boolean = false)

