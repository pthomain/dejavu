package uk.co.glass_software.android.cache_interceptor.retrofit.annotations

import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.OptionalBoolean.DEFAULT

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Offline(val freshOnly: Boolean = false,
                         val mergeOnNextOnError: OptionalBoolean = DEFAULT)
