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
                              val filterFinal: Boolean,
                              type: Type) : Operation(type) {

            class Cache(durationInMillis: Long? = null,
                        freshOnly: Boolean = false,
                        mergeOnNextOnError: Boolean? = null,
                        encrypt: Boolean? = null,
                        compress: Boolean? = null,
                        filterFinal: Boolean = false)
                : Expiring(
                    durationInMillis,
                    freshOnly,
                    mergeOnNextOnError,
                    encrypt,
                    compress,
                    filterFinal,
                    CACHE
            )

            class Refresh(durationInMillis: Long? = null,
                          freshOnly: Boolean = false,
                          mergeOnNextOnError: Boolean? = null,
                          filterFinal: Boolean = false)
                : Expiring(
                    durationInMillis,
                    freshOnly,
                    mergeOnNextOnError,
                    null,
                    null,
                    filterFinal,
                    REFRESH
            )
        }

        object Invalidate : Operation(INVALIDATE)

        data class Clear(val typeToClear: Class<*>? = null,
                         val clearOldEntriesOnly: Boolean = false) : Operation(CLEAR)

        enum class Type(val annotationName: String) {
            DO_NOT_CACHE("@DoNotCache"),
            CACHE("@Cache"),
            INVALIDATE("@Invalidate"),
            REFRESH("@Refresh"),
            CLEAR("@Clear"),
            CLEAR_ALL("@Clear")
        }
    }

}