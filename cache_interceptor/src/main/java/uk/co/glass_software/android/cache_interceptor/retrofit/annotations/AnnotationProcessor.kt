package uk.co.glass_software.android.cache_interceptor.retrofit.annotations

import uk.co.glass_software.android.cache_interceptor.configuration.CacheConfiguration
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation
import uk.co.glass_software.android.cache_interceptor.configuration.CacheInstruction.Operation.Type.*
import uk.co.glass_software.android.cache_interceptor.configuration.NetworkErrorProvider
import uk.co.glass_software.android.cache_interceptor.retrofit.annotations.AnnotationProcessor.RxType.COMPLETABLE

internal class AnnotationProcessor<E>(private val cacheConfiguration: CacheConfiguration<E>)
        where  E : Exception,
               E : NetworkErrorProvider {

    private val logger = cacheConfiguration.logger

    fun process(annotations: Array<Annotation>,
                rxType: AnnotationProcessor.RxType,
                responseClass: Class<*>): CacheInstruction? {
        if (annotations.isEmpty()
                && rxType != COMPLETABLE
                && cacheConfiguration.cacheAllByDefault) {

            logger.d(
                    "No annotation for call returning ${rxType.getTypedName(responseClass)} but cacheAllByDefault directive is set to true"
            )

            return CacheInstruction(
                    responseClass,
                    Operation.Expiring.Cache(
                            cacheConfiguration.cacheDurationInMillis,
                            false,
                            cacheConfiguration.mergeOnNextOnError,
                            cacheConfiguration.encrypt,
                            cacheConfiguration.compress,
                            false
                    )
            )
        }

        var instruction: CacheInstruction? = null
        annotations.forEach { annotation ->
            when (annotation) {
                is Cache -> CACHE
                is DoNotCache -> DO_NOT_CACHE
                is Invalidate -> INVALIDATE
                is Refresh -> REFRESH
                is Offline -> OFFLINE
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
        logger.e(this)
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
                    + " ${rxType.getTypedName(responseClass)}, found ${foundOperation.annotationName}"
                    + " after existing annotation ${currentInstruction.operation.type.annotationName}."
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
                    annotation.typeToInvalidate.java,
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

            is Offline -> CacheInstruction(
                    responseClass,
                    Operation.Expiring.Offline(
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

    class CacheInstructionException(message: String) : Exception(message)

    enum class RxType(private val className: String) {
        OBSERVABLE("Observable"),
        SINGLE("Single"),
        COMPLETABLE("Completable");

        fun getTypedName(responseClass: Class<*>) =
                if(this == COMPLETABLE) className
                else "$className<${responseClass.simpleName}>"
    }

}
