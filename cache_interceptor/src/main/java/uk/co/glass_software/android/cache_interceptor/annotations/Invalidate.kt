package uk.co.glass_software.android.cache_interceptor.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Invalidate(val typeToInvalidate: KClass<*>)
