package uk.co.glass_software.android.cache_interceptor.configuration

import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.*
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Expiring.*
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Type.*

internal object CacheInstructionSerialiser {

    private const val separator = ":"

    fun serialise(type: Operation.Type?,
                  vararg properties: Any?) =
            properties.joinToString(separator = separator) {
                when (it) {
                    is Class<*> -> it.canonicalName!!
                    else -> it.toString()
                }
            }.let {
                if (type == null) it
                else type.name + separator + it
            }

    fun deserialise(string: String) =
            if (string.contains(separator)) {
                string.split(separator).let { params ->
                    if (params.size >= 3) {
                        try {
                            when (params[1]) {
                                DO_NOT_CACHE.name -> DoNotCache
                                INVALIDATE.name -> Invalidate
                                CLEAR.name,
                                CLEAR_ALL.name -> getClearOperation(params)
                                CACHE.name,
                                REFRESH.name,
                                OFFLINE.name -> getExpiringOperation(params)
                                else -> null
                            }?.let { operation ->
                                getClassForName(params[0])?.let { responseClass ->
                                    CacheInstruction(responseClass, operation)
                                }
                            }
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                }
            } else null

    private fun getClassForName(value: String): Class<*>? = try {
        Class.forName(value)
    } catch (e: Exception) {
        null
    }

    private fun toBoolean(value: String) =
            if (value == "null") null else value == "true"

    private fun toLong(value: String) =
            if (value == "null") null else value.toLong()

    private fun getClearOperation(params: List<String>) =
            if (params.size == 4) {
                Clear(getClassForName(params[2]), params[3].toBoolean())
            } else null

    private fun getExpiringOperation(params: List<String>) =
            if (params.size == 8) {
                val durationInMillis = toLong(params[2])
                val freshOnly = toBoolean(params[3]) ?: false
                val mergeOnNextOnError = toBoolean(params[4])
                val encrypt = toBoolean(params[5])
                val compress = toBoolean(params[6])
                val filterFinal = toBoolean(params[7]) ?: false

                when (params[1]) {
                    CACHE.name -> Cache(
                            durationInMillis,
                            freshOnly,
                            mergeOnNextOnError,
                            encrypt,
                            compress,
                            filterFinal
                    )
                    REFRESH.name -> Refresh(
                            durationInMillis,
                            freshOnly,
                            mergeOnNextOnError,
                            filterFinal
                    )
                    OFFLINE.name -> Offline(
                            freshOnly,
                            mergeOnNextOnError
                    )
                    else -> throw IllegalArgumentException("Unknown type")
                }
            } else null

}