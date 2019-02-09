/*
 * Copyright (C) 2017 Glass Software Ltd
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package uk.co.glass_software.android.dejavu.test


import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.verify
import junit.framework.TestCase.*
import org.mockito.internal.verification.VerificationModeFactory
import org.mockito.internal.verification.api.VerificationData
import org.mockito.verification.VerificationMode
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.http.GET
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Clear
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.*
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Invalidate
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch
import uk.co.glass_software.android.dejavu.response.ResponseWrapper
import uk.co.glass_software.android.dejavu.retrofit.annotations.DoNotCache
import uk.co.glass_software.android.dejavu.test.network.model.TestResponse
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

fun <E> expectException(exceptionType: Class<E>,
                        message: String,
                        action: () -> Unit,
                        checkCause: Boolean = false) {
    try {
        action()
    } catch (e: Exception) {
        val toCheck = if (checkCause) e.cause else e

        if (toCheck != null && exceptionType == toCheck.javaClass) {
            assertEquals("The exception did not have the right message",
                    message,
                    toCheck.message
            )
            return
        } else {
            fail("Expected exception was not caught: $exceptionType, another one was caught instead $toCheck")
        }
    }

    fail("Expected exception was not caught: $exceptionType")
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
                                context: String? = null) =
        assertEquals(withContext(description, context), t1, t2)

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
        else "$context\n=> $description"

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

internal fun assertResponseWrapperWithContext(expected: ResponseWrapper<Glitch>,
                                              actual: ResponseWrapper<Glitch>,
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
                                   mode: VerificationMode = atLeastOnce(),
                                   context: String?) =
        verify(
                target,
                object : VerificationMode {
                    override fun verify(data: VerificationData?) {
                        mode.verify(data)
                    }

                    override fun description(description: String?) =
                            VerificationModeFactory.description(
                                    this,
                                    "$context\n=> $description"
                            )
                }
        )

fun assertArrayEqualsWithContext(expected: ByteArray?,
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

fun instructionToken(operation: CacheInstruction.Operation = Cache()) = CacheToken.fromInstruction(
        CacheInstruction(
                TestResponse::class.java,
                operation
        ),
        true,
        true,
        "/",
        null
)

fun operationSequence() = sequenceOf(
        Operation.DoNotCache,
        Invalidate,
        Clear(),
        Offline(true, mergeOnNextOnError = null),
        Offline(false, mergeOnNextOnError = null),
        Offline(true, mergeOnNextOnError = false),
        Offline(false, mergeOnNextOnError = false),
        Offline(true, mergeOnNextOnError = true),
        Offline(false, mergeOnNextOnError = true),
        Refresh(freshOnly = true, filterFinal = true, mergeOnNextOnError = null),
        Refresh(freshOnly = true, filterFinal = false, mergeOnNextOnError = null),
        Refresh(freshOnly = false, filterFinal = true, mergeOnNextOnError = null),
        Refresh(freshOnly = false, filterFinal = false, mergeOnNextOnError = null),
        Refresh(freshOnly = true, filterFinal = true, mergeOnNextOnError = false),
        Refresh(freshOnly = true, filterFinal = false, mergeOnNextOnError = false),
        Refresh(freshOnly = false, filterFinal = true, mergeOnNextOnError = false),
        Refresh(freshOnly = false, filterFinal = false, mergeOnNextOnError = false),
        Refresh(freshOnly = true, filterFinal = true, mergeOnNextOnError = true),
        Refresh(freshOnly = true, filterFinal = false, mergeOnNextOnError = true),
        Refresh(freshOnly = false, filterFinal = true, mergeOnNextOnError = true),
        Refresh(freshOnly = false, filterFinal = false, mergeOnNextOnError = true),
        Cache(freshOnly = true, filterFinal = true, mergeOnNextOnError = null),
        Cache(freshOnly = true, filterFinal = false, mergeOnNextOnError = null),
        Cache(freshOnly = false, filterFinal = true, mergeOnNextOnError = null),
        Cache(freshOnly = false, filterFinal = false, mergeOnNextOnError = null),
        Cache(freshOnly = true, filterFinal = true, mergeOnNextOnError = false),
        Cache(freshOnly = true, filterFinal = false, mergeOnNextOnError = false),
        Cache(freshOnly = false, filterFinal = true, mergeOnNextOnError = false),
        Cache(freshOnly = false, filterFinal = false, mergeOnNextOnError = false),
        Cache(freshOnly = true, filterFinal = true, mergeOnNextOnError = true),
        Cache(freshOnly = true, filterFinal = false, mergeOnNextOnError = true),
        Cache(freshOnly = false, filterFinal = true, mergeOnNextOnError = true),
        Cache(freshOnly = false, filterFinal = false, mergeOnNextOnError = true)
)

fun callAdapterFactory(rxClass: Class<*>,
                       retrofit: Retrofit,
                       constructor: Function3<Type, Array<Annotation>, Retrofit, CallAdapter<Any, Any>>) =
        constructor.invoke(
                object : ParameterizedType {
                    override fun getRawType() = rxClass
                    override fun getOwnerType() = null
                    override fun getActualTypeArguments() = arrayOf<Type>(TestResponse::class.java)
                },
                arrayOf(
                        getAnnotation<GET>(listOf("/")),
                        getAnnotation<DoNotCache>(emptyList())
                ),
                retrofit
        )