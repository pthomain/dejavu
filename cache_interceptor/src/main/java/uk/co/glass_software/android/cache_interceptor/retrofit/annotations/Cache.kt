package uk.co.glass_software.android.cache_interceptor.retrofit.annotations

import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.OptionalBoolean.DEFAULT

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Cache(val freshOnly: Boolean = false,
                       val durationInMillis: Long = -1L,
                       val mergeOnNextOnError: OptionalBoolean = DEFAULT,
                       val encrypt: OptionalBoolean = DEFAULT,
                       val compress: OptionalBoolean = DEFAULT)
