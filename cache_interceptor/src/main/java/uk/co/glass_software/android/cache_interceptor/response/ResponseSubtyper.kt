package uk.co.glass_software.android.cache_interceptor.response

import com.google.common.reflect.TypeToken
import net.bytebuddy.ByteBuddy


class ResponseSubtyper {

    private val byteBuddy = ByteBuddy()

    companion object {
        const val propertyName = "rxCacheInterceptorCacheMetadata"
        const val propertySetter = "setRxCacheInterceptorCacheMetadata"
        const val propertyGetter = "getRxCacheInterceptorCacheMetadata"
    }

    fun <R : Any> subtypeForMetadata(responseClass: Class<R>): Pair<Class<out Class<out R>>, (R, CacheMetadata) -> Unit> {
        val type = object : TypeToken<CacheMetadata>() {}.rawType
        val subtype = byteBuddy
                .subclass(responseClass)
                .defineProperty(propertyName, type)
                .make()
                .load(ClassLoader.getSystemClassLoader())
                .loaded

        return subtype::class.java to { subtypeInstance, metadata ->
            val setter = subtype.getDeclaredMethod(propertySetter, type)
            setter.invoke(subtypeInstance, metadata)
        }
    }

}
