package uk.co.glass_software.android.cache_interceptor.annotations

import uk.co.glass_software.android.cache_interceptor.annotations.OptionalBoolean.DEFAULT

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Refresh(val freshOnly: Boolean = false,
                         val durationInMillis: Long = -1L,
                         val mergeOnNextOnError: OptionalBoolean = DEFAULT)

