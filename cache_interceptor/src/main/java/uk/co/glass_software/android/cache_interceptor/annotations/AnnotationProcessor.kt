package uk.co.glass_software.android.cache_interceptor.annotations

import uk.co.glass_software.android.cache_interceptor.annotations.AnnotationProcessor.RxType.COMPLETABLE
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation
import uk.co.glass_software.android.cache_interceptor.annotations.CacheInstruction.Operation.Type.*
import uk.co.glass_software.android.cache_interceptor.configuration.CacheConfiguration
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider

internal class AnnotationProcessor<E>(private val cacheConfiguration: CacheConfiguration<E>)
        where  E : Exception,
               E : NetworkErrorProvider {

    enum class RxType(private val className: String) {
        OBSERVABLE("Observable"),
        SINGLE("Single"),
        COMPLETABLE("Completable");

        fun getTypedName(responseClass: Class<*>) =
                "$className<${responseClass.simpleName}>"
    }

    fun process(annotations: Array<Annotation>,
                rxType: AnnotationProcessor.RxType,
                responseClass: Class<*>): CacheInstruction? {
        var instruction: CacheInstruction? = null

        if (annotations.isEmpty()
                && rxType != COMPLETABLE
                && cacheConfiguration.cacheAllByDefault) {

            cacheConfiguration.logger.d(
                    "No annotation for call returning ${rxType.getTypedName(responseClass)} but cacheAllByDefault directive is set to true"
            )

            return CacheInstruction(
                    responseClass,
                    CacheInstruction.Operation.Expiring.Cache(
                            cacheConfiguration.cacheDurationInMillis,
                            false,
                            cacheConfiguration.mergeOnNextOnError,
                            cacheConfiguration.encrypt,
                            cacheConfiguration.compress,
                            false
                    )
            )
        }

        annotations.forEach { annotation ->
            when (annotation) {
                is Cache -> CACHE
                is DoNotCache -> DO_NOT_CACHE
                is Invalidate -> INVALIDATE
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
            val completableOperations = arrayOf(
                    CLEAR,
                    CLEAR_ALL,
                    INVALIDATE
            )

            if (completableOperations.contains(operation)) {
                if (rxType != COMPLETABLE) {
                    CacheInstructionException(
                            "Only Completable can be used with the ${operation.annotationName} annotation"
                    ).logAndThrow()
                }
            } else {
                if (rxType == COMPLETABLE) {
                    CacheInstructionException(
                            "Only @Clear, @ClearAll or @Invalidate annotations can be used with Completable"
                    ).logAndThrow()
                }
            }
        }

        return instruction
    }

    private fun CacheInstructionException.logAndThrow() {
        cacheConfiguration.logger.e(this)
        throw this
    }

    @Throws(CacheInstructionException::class)
    private fun getInstruction(currentInstruction: CacheInstruction?,
                               rxType: RxType,
                               responseClass: Class<*>,
                               foundOperation: Operation.Type,
                               annotation: Annotation): CacheInstruction? {
        if (currentInstruction != null) {
            CacheInstructionException("More than one cache annotation defined for method returning"
                    + " ${rxType.getTypedName(responseClass)}, found ${getAnnotationName(foundOperation)}"
                    + " after existing annotation ${getAnnotationName(currentInstruction.operation.type)}."
                    + " Only one annotation can be used for this method."
            ).logAndThrow()
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

            is Invalidate -> CacheInstruction(
                    annotation.typeToClear.java,
                    Operation.Invalidate
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
        DO_NOT_CACHE -> "@DoNotCache"
        INVALIDATE -> "@Invalidate"
        REFRESH -> "@Refresh"
        CLEAR -> "@Clear"
        CLEAR_ALL -> "@ClearAll"
    }

    class CacheInstructionException(message: String) : Exception(message)

}
