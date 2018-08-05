package uk.co.glass_software.android.cache_interceptor.annotations

import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation

internal class AnnotationHelper {

    fun process(annotations: Array<Annotation>,
                isSingle: Boolean,
                responseClass: Class<*>): CacheInstruction? {
        var instruction: CacheInstruction? = null

        annotations.forEach {
            when (it) {
                is Cache -> instruction = getInstruction(
                        instruction,
                        isSingle,
                        responseClass,
                        Operation.Type.CACHE,
                        it
                )

                is Refresh -> instruction = getInstruction(
                        instruction,
                        isSingle,
                        responseClass,
                        Operation.Type.REFRESH,
                        it
                )

                is Clear -> instruction = getInstruction(
                        instruction,
                        isSingle,
                        responseClass,
                        Operation.Type.CLEAR,
                        it
                )
            }
        }

        return instruction
    }

    @Throws(CacheInstructionException::class)
    private fun getInstruction(currentInstruction: CacheInstruction?,
                               isSingle: Boolean,
                               responseClass: Class<*>,
                               foundOperation: Operation.Type,
                               annotation: Annotation): CacheInstruction? {
        if (currentInstruction != null) {
            val responseClassName = responseClass.simpleName
            val signature = if (isSingle) "Single<$responseClassName>" else "Observable<$responseClassName>"

            throw CacheInstructionException("More than one cache annotation defined for method returning $signature, " +
                    "found ${getAnnotationName(foundOperation)} after existing annotation ${getAnnotationName(currentInstruction.operation.type)}. " +
                    "Only one annotation can be used for this method.")
        }

        return when (annotation) {
            is Cache -> CacheInstruction(
                    responseClass,
                    Operation.Expiring.Cache(
                            annotation.durationInMillis,
                            annotation.freshOnly,
                            annotation.encrypt,
                            annotation.compress
                    )
            )

            is Refresh -> CacheInstruction(
                    responseClass,
                    Operation.Expiring.Refresh(
                            annotation.durationInMillis,
                            annotation.freshOnly
                    )
            )

            is Clear -> {
                CacheInstruction(
                        responseClass,
                        Operation.Clear(
                                annotation.typeToClear.java,
                                annotation.clearOldEntriesOnly,
                                annotation.clearEntireCache
                        )
                )
            }

            else -> null
        }
    }

    private fun getAnnotationName(foundOperation: Operation.Type): String = when (foundOperation) {
        Operation.Type.CACHE -> "@Cache"
        Operation.Type.REFRESH -> "@Refresh"
        Operation.Type.CLEAR -> "@Clear"
        else -> ""
    }

    class CacheInstructionException(message: String) : Exception(message)

}
