/*
 *
 *  Copyright (C) 2017 Pierre Thomain
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package dev.pthomain.android.dejavu.test

import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction
import dev.pthomain.android.dejavu.configuration.instruction.Operation
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Clear
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Expiring
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import junit.framework.TestCase
import kotlin.reflect.full.createInstance

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

fun cacheInstruction(operation: Operation) =
        CacheInstruction(TestResponse::class.java, operation)

inline fun <reified T : Annotation> getAnnotationParams(args: List<Any?>?) =
        if (args == null) emptyMap()
        else T::class.constructors
                .first()
                .parameters
                .mapIndexed { index, param -> Pair(param, args[index]) }
                .toMap()

inline fun <reified T : Annotation> getAnnotation(args: List<Any?>) =
        if (args.isNullOrEmpty()) T::class.createInstance()
        else T::class.constructors.first().callBy(getAnnotationParams<T>(args))
