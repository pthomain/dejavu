package uk.co.glass_software.android.cache_interceptor.demo

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Type.*

class InstructionView @JvmOverloads constructor(context: Context,
                                                attrs: AttributeSet? = null,
                                                defStyleAttr: Int = 0)
    : TextView(context, attrs, defStyleAttr) {

    //TODO add colour
    fun setInstruction(instruction: CacheInstruction) {
        text = instruction.operation.type.annotationName.let {
            getRestMethod(instruction.operation)
                    .plus(it)
                    .plus(getDirectives(it.length + 1, instruction.operation))
                    .plus(getMethod(instruction))
        }
    }

    private fun getRestMethod(operation: Operation) =
            when (operation.type) {
                DO_NOT_CACHE,
                CACHE,
                REFRESH,
                OFFLINE -> "@GET(\"fact\")"

                INVALIDATE,
                CLEAR,
                CLEAR_ALL -> "@DELETE(\"fact\")"
            } + "\n"

    private fun getDirectives(length: Int,
                              operation: Operation) =
            "".padStart(length, ' ')
                    .let { padding ->
                        when (operation.type) {
                            CACHE -> (operation as Operation.Expiring.Cache).let { cacheOperation ->
                                arrayOf(
                                        "freshOnly = ${cacheOperation.freshOnly}",
                                        "durationInMillis = ${cacheOperation.durationInMillis
                                                ?: "-1L"}",
                                        "mergeOnNextOnError = ${getOptionalBoolean(cacheOperation.mergeOnNextOnError)}",
                                        "encrypt = ${getOptionalBoolean(cacheOperation.encrypt)}",
                                        "compress = ${getOptionalBoolean(cacheOperation.compress)}"
                                )
                            }

                            REFRESH -> (operation as Operation.Expiring.Refresh).let { refreshOperation ->
                                arrayOf(
                                        "freshOnly = ${refreshOperation.freshOnly}",
                                        "durationInMillis = ${refreshOperation.durationInMillis
                                                ?: "-1L"}",
                                        "mergeOnNextOnError = ${getOptionalBoolean(refreshOperation.mergeOnNextOnError)}"
                                )
                            }

                            OFFLINE -> (operation as Operation.Expiring.Offline).let { offlineOperation ->
                                arrayOf(
                                        "freshOnly = ${offlineOperation.freshOnly}",
                                        "mergeOnNextOnError = ${getOptionalBoolean(offlineOperation.mergeOnNextOnError)}"
                                )
                            }

                            CLEAR,
                            CLEAR_ALL -> arrayOf(
                                    "clearOldEntriesOnly = ${(operation as Operation.Clear).clearOldEntriesOnly}"
                            )

                            INVALIDATE,
                            DO_NOT_CACHE -> emptyArray()
                        }.let { directives ->
                            directives.map { padding + it }
                        }.joinToString(separator = ",\n").trim().let {
                            if (it.isEmpty()) "" else "($it)"
                        }
                    }

    private fun getOptionalBoolean(optional: Boolean?) =
            "OptionalBoolean.".plus(
                    when {
                        optional == null -> "DEFAULT"
                        optional -> "TRUE"
                        else -> "FALSE"
                    }
            )

    private fun getMethod(instruction: CacheInstruction) =
            "\nfun call() : " + when (instruction.operation.type) {
                CACHE,
                REFRESH -> "Observable<${instruction.responseClass.simpleName}>"

                DO_NOT_CACHE,
                OFFLINE -> "Single<${instruction.responseClass.simpleName}>"

                INVALIDATE,
                CLEAR,
                CLEAR_ALL -> "Completable"
            }
}