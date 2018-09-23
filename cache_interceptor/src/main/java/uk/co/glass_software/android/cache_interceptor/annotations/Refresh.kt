package uk.co.glass_software.android.cache_interceptor.annotations


@Target(AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Refresh(val durationInMillis: Long = DEFAULT_DURATION,
                         val mergeOnNextOnError: Boolean = false,
                         val freshOnly: Boolean = false)

