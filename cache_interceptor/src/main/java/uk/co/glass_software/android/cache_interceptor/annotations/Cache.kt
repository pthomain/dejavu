package uk.co.glass_software.android.cache_interceptor.annotations

@Target(AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Cache(val durationInMillis: Long = DEFAULT_DURATION,
                       val mergeOnNextOnError: Boolean = false,
                       val freshOnly: Boolean = false,
                       val encrypt: Boolean = false,
                       val compress: Boolean = true)

const val DEFAULT_DURATION = 300000L //5 min
