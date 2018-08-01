package uk.co.glass_software.android.cache_interceptor.annotations

import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.*

data class CacheInstruction(val responseClass: Class<*>,
                            val operation: Operation,
                            val mergeOnNextOnError: Boolean = false,
                            val strictMode: Boolean = false) {

    sealed class Operation(val type: Type) {

        object DoNotCache : Operation(DO_NOT_CACHE)

        sealed class Expiring(val durationInMillis: Float = DEFAULT_DURATION,
                              val freshOnly: Boolean = false,
                              type: Type) : Operation(type) {

            class Cache(durationInMillis: Float = DEFAULT_DURATION,
                        freshOnly: Boolean = false,
                        val encrypt: Boolean = false,
                        val compress: Boolean = false)
                : Expiring(durationInMillis, freshOnly, CACHE)

            class Refresh(durationInMillis: Float = DEFAULT_DURATION,
                          freshOnly: Boolean = false)
                : Expiring(durationInMillis, freshOnly, REFRESH)
        }

        data class Clear(val typeToClear: Class<*>? = null,
                         val clearOldEntriesOnly: Boolean = false,
                         val clearEntireCache: Boolean = false) : Operation(CLEAR)

        enum class Type {
            DO_NOT_CACHE,
            CACHE,
            REFRESH,
            CLEAR
        }
    }

}