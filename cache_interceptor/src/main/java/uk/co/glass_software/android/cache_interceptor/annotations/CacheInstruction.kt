package uk.co.glass_software.android.cache_interceptor.annotations

import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.*

data class CacheInstruction(val responseClass: Class<*>,
                            val operation: Operation) {

    sealed class Operation(val type: Type) {

        object DoNotCache : Operation(DO_NOT_CACHE)

        sealed class Expiring(val durationInMillis: Long?,
                              val freshOnly: Boolean,
                              val mergeOnNextOnError: Boolean?,
                              val encrypt: Boolean?,
                              val compress: Boolean?,
                              type: Type) : Operation(type) {

            class Cache(durationInMillis: Long?,
                        freshOnly: Boolean,
                        mergeOnNextOnError: Boolean?,
                        encrypt: Boolean?,
                        compress: Boolean?)
                : Expiring(
                    durationInMillis,
                    freshOnly,
                    mergeOnNextOnError,
                    encrypt,
                    compress,
                    CACHE
            )

            class Refresh(durationInMillis: Long?,
                          freshOnly: Boolean,
                          mergeOnNextOnError: Boolean?)
                : Expiring(
                    durationInMillis,
                    freshOnly,
                    mergeOnNextOnError,
                    null, //TODO keep settings from original cache call
                    null,
                    REFRESH
            )
        }

        data class Clear(val typeToClear: Class<*>?,
                         val clearOldEntriesOnly: Boolean) : Operation(CLEAR)

        enum class Type {
            DO_NOT_CACHE,
            CACHE,
            REFRESH,
            CLEAR,
            CLEAR_ALL
        }
    }

}