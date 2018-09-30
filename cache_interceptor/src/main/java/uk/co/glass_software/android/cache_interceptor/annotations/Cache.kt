package uk.co.glass_software.android.cache_interceptor.annotations

import uk.co.glass_software.android.cache_interceptor.annotations.OptionalBoolean.DEFAULT

@Target(AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Cache(val freshOnly: Boolean = false,
                       val durationInMillis: Long = -1L,
                       val mergeOnNextOnError: OptionalBoolean = DEFAULT,
                       val encrypt: OptionalBoolean = DEFAULT,
                       val compress: OptionalBoolean = DEFAULT)
