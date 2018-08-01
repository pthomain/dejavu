package uk.co.glass_software.android.cache_interceptor.annotations

@Target(AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Cache(val durationInMillis: Float = DEFAULT_DURATION,
                       val freshOnly: Boolean = false,
                       val encrypt: Boolean = false,
                       val compress: Boolean = true,
                       val mergeOnNextOnError: Boolean = false)

const val DEFAULT_DURATION = 300000F //5 min
