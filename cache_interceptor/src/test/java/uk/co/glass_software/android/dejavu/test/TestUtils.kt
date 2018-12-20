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


import junit.framework.TestCase.*
import kotlin.reflect.full.createInstance

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
        else "$context => $description"


inline fun <reified T : Annotation> getAnnotationParams(args: List<Any?>) =
        T::class.constructors
                .first()
                .parameters
                .mapIndexed { index, param -> Pair(param, args[index]) }
                .toMap()

inline fun <reified T : Annotation> getAnnotation(args: List<Any?>) =
        if (args.isNullOrEmpty()) T::class.createInstance()
        else T::class.constructors.first().callBy(getAnnotationParams<T>(args))
