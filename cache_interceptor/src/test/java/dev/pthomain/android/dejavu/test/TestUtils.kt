/*
 *
 *  Copyright (C) 2017-2020 Pierre Thomain
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


import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import dev.pthomain.android.dejavu.injection.integration.module.NOW
import dev.pthomain.android.dejavu.interceptors.cache.instruction.*
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.CachePriority
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Local.Invalidate
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheStatus.*
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.RequestToken
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.ResponseToken
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.retrofit.annotations.DoNotCache
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import junit.framework.TestCase.*
import org.junit.Assert.assertArrayEquals
import org.mockito.internal.verification.VerificationModeFactory
import org.mockito.verification.VerificationMode
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.http.GET
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

fun <E> expectException(exceptionType: Class<E>,
                        message: String,
                        action: () -> Unit,
                        context: String? = null,
                        checkCause: Boolean = false) {
    try {
        action()
    } catch (e: Exception) {
        val toCheck = if (checkCause) e.cause else e

        if (toCheck != null && exceptionType == toCheck.javaClass) {
            assertEquals(
                    withContext("The exception did not have the right message", context),
                    message,
                    toCheck.message
            )
            return
        } else {
            fail(withContext(
                    "Expected exception was not caught: $exceptionType, another one was caught instead $toCheck",
                    context
            ))
        }
    }

    fail(withContext("Expected exception was not caught: $exceptionType", context))
}

fun assertTrueWithContext(assumption: Boolean,
                          description: String,
                          context: String? = null) =
        assertTrue(withContext(description, context), assumption)

fun assertFalseWithContext(assumption: Boolean,
                           description: String,
                           context: String? = null) =
        assertFalse(withContext(description, context), assumption)

fun <T> assertEqualsWithContext(t1: T,
                                t2: T,
                                description: String,
                                context: String? = null) {
    val withContext = withContext(description, context)

    when {
        t1 is Array<*> && t2 is Array<*> -> assertArrayEquals(withContext, t1, t2)
        t1 is ByteArray && t2 is ByteArray -> assertByteArrayEqualsWithContext(t1, t2, context)
        else -> assertEquals(withContext, t1, t2)
    }
}

fun <T> assertNullWithContext(value: T?,
                              description: String,
                              context: String? = null) =
        assertNull(withContext(description, context), value)

fun <T> assertNotNullWithContext(value: T?,
                                 description: String,
                                 context: String? = null) =
        assertNotNull(withContext(description, context), value)

fun failWithContext(description: String,
                    context: String? = null) {
    fail(withContext(description, context))
}

fun withContext(description: String,
                context: String? = null) =
        if (context == null) description
        else "\n$context\n=> $description"

fun assertGlitchWithContext(expectedGlitch: Glitch?,
                            actualGlitch: Any?,
                            context: String? = null) {
    assertTrueWithContext(
            actualGlitch is Glitch,
            withContext("Value was not a Glitch", context)
    )

    actualGlitch as Glitch?

    val expectedCause = expectedGlitch?.cause
    val actualCause = actualGlitch?.cause

    if (expectedCause == null) {
        assertTrueWithContext(
                actualCause == null,
                "Glitch cause should be null"
        )
    } else {
        assertFalseWithContext(
                actualCause == null,
                "Glitch cause shouldn't be null"
        )

        assertTrueWithContext(
                expectedCause.javaClass == actualCause!!.javaClass,
                "Glitch cause type was different"
        )

        assertTrueWithContext(
                expectedCause.message == actualCause.message,
                "Glitch cause message was different"
        )

        assertEqualsWithContext(
                expectedGlitch.httpStatus,
                actualGlitch?.httpStatus,
                withContext("Glitch httpStatus didn't match", context)
        )

        assertEqualsWithContext(
                expectedGlitch.errorCode,
                actualGlitch?.errorCode,
                withContext("Glitch errorCode didn't match", context)
        )

        assertEqualsWithContext(
                expectedGlitch.description,
                actualGlitch?.description,
                withContext("Glitch description didn't match", context)
        )
    }
}

internal fun assertResponseWrapperWithContext(expected: ResponseWrapper<*, *, Glitch>,
                                              actual: ResponseWrapper<*, *, Glitch>,
                                              context: String? = null) {
    assertEqualsWithContext(
            expected.responseClass,
            actual.responseClass,
            "Response class didn't match",
            context
    )

    assertEqualsWithContext(
            expected.response,
            actual.response,
            "Responses didn't match",
            context
    )

    assertEqualsWithContext(
            expected.metadata,
            actual.metadata,
            "Response metadata didn't match",
            context
    )
}

internal fun <T> verifyWithContext(target: T,
                                   context: String?) =
        verifyAndLogContext(
                target,
                VerificationModeFactory.description(
                        atLeastOnce(),
                        "\n$context"
                ),
                context
        )

internal fun <T> verifyNeverWithContext(target: T,
                                        context: String?) =
        verifyAndLogContext(
                target,
                VerificationModeFactory.description(
                        never(),
                        "\n$context"
                ),
                context
        )

private fun <T> verifyAndLogContext(target: T,
                                    mode: VerificationMode,
                                    context: String?): T =
        try {
            verify(target, mode)
        } catch (e: Throwable) {
            context?.also { println(it) }
            target
        }

fun assertByteArrayEqualsWithContext(expected: ByteArray?,
                                     other: ByteArray?,
                                     context: String? = null) {
    when {
        expected == null -> assertNullWithContext(
                other,
                "Byte array should be null",
                context
        )
        other != null && other.size == expected.size -> {
            other.forEachIndexed { index, byte ->
                if (expected[index] != byte) {
                    assertEqualsWithContext(
                            expected[index],
                            byte,
                            "Byte didn't match at index $index",
                            context
                    )
                }
            }
        }
        else -> failWithContext(
                "Byte array had the wrong size",
                context
        )
    }
}

internal fun defaultResponseWrapper(metadata: ResponseMetadata<Cache, ResponseToken<Cache>, Glitch>,
                                    response: TestResponse?) = ResponseWrapper(
        TestResponse::class.java,
        response,
        metadata
)

fun defaultRequestMetadata() = PlainRequestMetadata(
        TestResponse::class.java,
        DEFAULT_URL
)

fun instructionToken(operation: Operation = Cache()) = RequestToken(
        CacheInstruction(
                operation,
                ValidRequestMetadata(
                        TestResponse::class.java,
                        DEFAULT_URL,
                        null,
                        INVALID_HASH,
                        INVALID_HASH
                )
        ),
        NETWORK,
        NOW
)

fun prioritySequence(action: (CachePriority) -> Unit) =
        CachePriority.values().asSequence()
                .forEach(action::invoke)

inline fun operationSequence(action: (Operation) -> Unit) {
    sequenceOf(
            Remote.DoNotCache,
            Invalidate,
            Clear(),
            Clear(true)
    ).plus(CachePriority.values().asSequence().map { Cache(it, encrypt = true, compress = true) })
            .forEach(action)
}

inline fun trueFalseSequence(action: (Boolean) -> Unit) {
    sequenceOf(true, false).forEach(action)
}

inline fun cacheStatusSequence(action: (CacheStatus) -> Unit) {
    CacheStatus.values().forEach(action)
}

fun isStatusValid(cacheStatus: CacheStatus,
                  operation: Operation) = when (operation) {

    is Cache -> operation.priority.returnStatuses.contains(cacheStatus)

    Remote.DoNotCache -> cacheStatus == NOT_CACHED

    is Invalidate,
    is Clear -> cacheStatus == DONE
}

inline fun operationAndStatusSequence(action: (Pair<Operation, CacheStatus>) -> Unit) {
    operationSequence { operation ->
        cacheStatusSequence { cacheStatus ->
            if (isStatusValid(cacheStatus, operation)) {
                action(operation to cacheStatus)
            }
        }
    }
}

fun callAdapterFactory(rxClass: Class<*>,
                       retrofit: Retrofit,
                       targetClass: Class<*>,
                       constructor: (Type, Array<Annotation>, Retrofit) -> CallAdapter<Any, Any>) =
        constructor.invoke(
                object : ParameterizedType {
                    override fun getRawType() = rxClass
                    override fun getOwnerType() = null
                    override fun getActualTypeArguments() = arrayOf<Type>(targetClass)
                },
                arrayOf(
                        getAnnotation<GET>(listOf("/")),
                        getAnnotation<DoNotCache>(emptyList())
                ),
                retrofit
        )