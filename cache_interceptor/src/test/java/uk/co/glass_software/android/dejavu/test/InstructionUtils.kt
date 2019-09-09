package uk.co.glass_software.android.dejavu.test

import junit.framework.TestCase
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Clear
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import kotlin.reflect.full.createInstance

fun assertInstruction(expectedInstruction: CacheInstruction,
                      actualInstruction: CacheInstruction?,
                      context: String? = null) {
    assertEqualsWithContext(
            expectedInstruction.responseClass,
            actualInstruction?.responseClass,
            "Instruction response class didn't match",
            context
    )

    assertInstruction(
            expectedInstruction.operation,
            actualInstruction,
            context
    )
}

fun assertInstruction(expectedOperation: Operation,
                      actualInstruction: CacheInstruction?,
                      context: String? = null) {
    TestCase.assertNotNull(
            withContext("Instruction shouldn't be null", context),
            actualInstruction
    )

    actualInstruction?.apply {
        assertEqualsWithContext(
                responseClass,
                TestResponse::class.java,
                "Response class should be TestResponse",
                context
        )

        assertOperation(
                expectedOperation,
                operation,
                withContext("Operation was wrong", context)
        )
    }
}

fun assertOperation(expectedOperation: Operation,
                    actualOperation: Operation,
                    context: String? = null) {
    assertEqualsWithContext(
            expectedOperation.type,
            actualOperation.type,
            "Wrong operation type",
            context
    )

    when (expectedOperation) {
        is Clear -> assertClear(
                expectedOperation,
                actualOperation as Clear,
                context
        )

        is Expiring -> assertExpiring(
                expectedOperation,
                actualOperation as Expiring,
                context
        )
    }
}

private fun assertExpiring(expectedOperation: Expiring,
                           actualOperation: Expiring,
                           context: String? = null) {
    assertEqualsWithContext(
            expectedOperation.durationInMillis,
            actualOperation.durationInMillis,
            "durationInMillis didn't match",
            context
    )

    assertEqualsWithContext(
            expectedOperation.connectivityTimeoutInMillis,
            actualOperation.connectivityTimeoutInMillis,
            "connectivityTimeoutInMillis didn't match",
            context
    )

    assertEqualsWithContext(
            expectedOperation.freshOnly,
            actualOperation.freshOnly,
            "freshOnly didn't match",
            context
    )

    assertEqualsWithContext(
            expectedOperation.mergeOnNextOnError,
            actualOperation.mergeOnNextOnError,
            "mergeOnNextOnError didn't match",
            context
    )

    assertEqualsWithContext(
            expectedOperation.encrypt,
            actualOperation.encrypt,
            "encrypt didn't match",
            context
    )

    assertEqualsWithContext(
            expectedOperation.compress,
            actualOperation.compress,
            "compress didn't match",
            context
    )

    assertEqualsWithContext(
            expectedOperation.filterFinal,
            actualOperation.filterFinal,
            "filterFinal didn't match",
            context
    )
}

private fun assertClear(expectedOperation: Clear,
                        actualOperation: Clear,
                        context: String? = null) {
    assertEqualsWithContext(
            expectedOperation.clearStaleEntriesOnly,
            actualOperation.clearStaleEntriesOnly,
            "clearStaleEntriesOnly didn't match",
            context
    )

    assertEqualsWithContext(
            expectedOperation.typeToClear,
            actualOperation.typeToClear,
            "typeToClear didn't match",
            context
    )
}

fun cacheInstruction(operation: CacheInstruction.Operation) =
        CacheInstruction(TestResponse::class.java, operation)

inline fun <reified T : Annotation> getAnnotationParams(args: List<Any?>) =
        T::class.constructors
                .first()
                .parameters
                .mapIndexed { index, param -> Pair(param, args[index]) }
                .toMap()

inline fun <reified T : Annotation> getAnnotation(args: List<Any?>) =
        if (args.isNullOrEmpty()) T::class.createInstance()
        else T::class.constructors.first().callBy(getAnnotationParams<T>(args))
