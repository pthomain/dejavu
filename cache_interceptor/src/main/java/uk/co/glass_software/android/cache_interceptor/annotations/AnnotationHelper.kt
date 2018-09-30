package uk.co.glass_software.android.cache_interceptor.annotations

import uk.co.glass_software.android.cache_interceptor.annotations.AnnotationHelper.RxType.*
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.*

internal class AnnotationHelper {

    enum class RxType {
        OBSERVABLE,
        SINGLE,
        COMPLETABLE
    }

    fun process(annotations: Array<Annotation>,
                rxType: RxType,
                responseClass: Class<*>): CacheInstruction? {
        var instruction: CacheInstruction? = null

        annotations.forEach { annotation ->
            when (annotation) {
                is Cache -> CACHE
                is Refresh -> REFRESH
                is Clear -> CLEAR
                is ClearAll -> CLEAR_ALL
                else -> null
            }?.let { operation ->
                instruction = getInstruction(
                        instruction,
                        rxType,
                        responseClass,
                        operation,
                        annotation
                )
            }
        }

        instruction?.let {
            val operation = it.operation.type
            if (rxType == COMPLETABLE && operation != CLEAR && operation != CLEAR_ALL) {
                throw CacheInstructionException("Only @Clear or @ClearAll annotations can be used with Completable")
            }
        }

        return instruction
    }

    @Throws(CacheInstructionException::class)
    private fun getInstruction(currentInstruction: CacheInstruction?,
                               rxType: RxType,
                               responseClass: Class<*>,
                               foundOperation: Operation.Type,
                               annotation: Annotation): CacheInstruction? {
        if (currentInstruction != null) {
            val responseClassName = responseClass.simpleName
            val signature = when (rxType) {
                OBSERVABLE -> "Observable<$responseClassName>"
                SINGLE -> "Single<$responseClassName>"
                COMPLETABLE -> "Completable"
            }

            throw CacheInstructionException("More than one cache annotation defined for method returning $signature, " +
                    "found ${getAnnotationName(foundOperation)} after existing annotation ${getAnnotationName(currentInstruction.operation.type)}. " +
                    "Only one annotation can be used for this method.")
        }

        return when (annotation) {
            is Cache -> CacheInstruction(
                    responseClass,
                    Operation.Expiring.Cache(
                            annotation.durationInMillis.let { if (it == -1L) null else it },
                            annotation.freshOnly,
                            annotation.mergeOnNextOnError.value,
                            annotation.encrypt.value,
                            annotation.compress.value
                    )
            )

            is Refresh -> CacheInstruction(
                    responseClass,
                    Operation.Expiring.Refresh(
                            annotation.durationInMillis.let { if (it == -1L) null else it },
                            annotation.freshOnly,
                            annotation.mergeOnNextOnError.value
                    )
            )

            is Clear -> {
                CacheInstruction(
                        annotation.typeToClear.java,
                        Operation.Clear(
                                annotation.typeToClear.java,
                                annotation.clearOldEntriesOnly
                        )
                )
            }

            is ClearAll -> {
                CacheInstruction(
                        responseClass,
                        Operation.Clear(
                                null,
                                annotation.clearOldEntriesOnly
                        )
                )
            }

            else -> null
        }
    }

    private fun getAnnotationName(foundOperation: Operation.Type): String = when (foundOperation) {
        CACHE -> "@Cache"
        REFRESH -> "@Refresh"
        CLEAR -> "@Clear"
        CLEAR_ALL -> "@ClearAll"
        else -> ""
    }

    class CacheInstructionException(message: String) : Exception(message)

}
