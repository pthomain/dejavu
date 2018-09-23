package uk.co.glass_software.android.cache_interceptor.annotations

import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.*

data class CacheInstruction(val responseClass: Class<*>,
                            val operation: Operation,
                            val strictMode: Boolean = false) {

    sealed class Operation(val type: Type) {

        object DoNotCache : Operation(DO_NOT_CACHE)

        sealed class Expiring(val durationInMillis: Long = DEFAULT_DURATION,
                              val freshOnly: Boolean = false,
                              val mergeOnNextOnError: Boolean = false,
                              val encrypt: Boolean = false,
                              val compress: Boolean = false,
                              type: Type) : Operation(type) {

            class Cache(durationInMillis: Long = DEFAULT_DURATION,
                        freshOnly: Boolean = false,
                        mergeOnNextOnError: Boolean = false,
                        encrypt: Boolean = false,
                        compress: Boolean = false)
                : Expiring(
                    durationInMillis,
                    freshOnly,
                    mergeOnNextOnError,
                    encrypt,
                    compress,
                    CACHE
            )

            class Refresh(durationInMillis: Long = DEFAULT_DURATION,
                          freshOnly: Boolean = false,
                          mergeOnNextOnError: Boolean = false,
                          encrypt: Boolean = false,
                          compress: Boolean = false)
                : Expiring(
                    durationInMillis,
                    freshOnly,
                    mergeOnNextOnError,
                    encrypt,
                    compress,
                    REFRESH
            )
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