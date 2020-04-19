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

package dev.pthomain.android.dejavu.retrofit.annotations

import com.nhaarman.mockitokotlin2.mock
import dev.pthomain.android.DejaVu.Configuration.Companion.DEFAULT_CACHE_DURATION_IN_SECONDS
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.CachePriority
import dev.pthomain.android.dejavu.cache.metadata.token.instruction.operation.Operation
import dev.pthomain.android.dejavu.retrofit.annotations.processor.AnnotationProcessor
import dev.pthomain.android.dejavu.retrofit.annotations.processor.CacheException
import dev.pthomain.android.dejavu.test.*
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import dev.pthomain.android.glitchy.interceptor.error.glitch.Glitch
import org.junit.Before
import org.junit.Test

class AnnotationProcessorUnitTest {

    private val responseKClass = TestResponse::class
    private val responseClass = responseKClass.java

    private lateinit var target: AnnotationProcessor<Glitch>

    @Before
    fun setUp() {
        target = AnnotationProcessor(mock())
    }

    @Test
    fun testProcessWithNoAnnotations() {
        val instruction = target.process(
                arrayOf(),
                OBSERVABLE,
                responseClass
        )

        assertNullWithContext(
                instruction,
                "Instruction should be null when no annotations are present"
        )
    }

    @Test
    fun testProcessWithTwoAnnotations() {
        val expectedErrorMessage = ("More than one cache annotation defined for method returning"
                + " Observable<TestResponse>, found @Cache after existing annotation @DoNotCache."
                + " Only one annotation can be used for this method.")

        expectException(
                CacheException::class.java,
                expectedErrorMessage,
                {
                    target.process(
                            arrayOf(
                                    getAnnotation<DoNotCache>(emptyList()),
                                    getAnnotation<Cache>(emptyList())
                            ),
                            OBSERVABLE,
                            responseClass
                    )
                }
        )
    }

    @Test
    fun testProcessCache() {
        prioritySequence { priority ->
            trueFalseSequence { encrypt ->
                trueFalseSequence { compress ->
                    trueFalseSequence { useDefaultDurations ->
                        testProcessCache(
                                priority,
                                encrypt,
                                compress,
                                useDefaultDurations
                        )
                    }
                }
            }
        }
    }

    private fun testProcessCache(priority: CachePriority,
                                 encrypt: Boolean,
                                 compress: Boolean,
                                 useDefaultDurations: Boolean) {
        val durationInSeconds = ifElse(useDefaultDurations, -1, 123)
        val connectivityTimeoutInSeconds = ifElse(useDefaultDurations, -1, 234)
        val requestTimeOutInSeconds = ifElse(useDefaultDurations, -1, 345)

        testProcessAnnotation(
                getAnnotation<Cache>(listOf(
                        priority,
                        durationInSeconds,
                        connectivityTimeoutInSeconds,
                        requestTimeOutInSeconds,
                        encrypt,
                        compress
                )),
                Operation.Cache(
                        priority,
                        ifElse(useDefaultDurations, DEFAULT_CACHE_DURATION_IN_SECONDS, durationInSeconds),
                        ifElse(useDefaultDurations, null, connectivityTimeoutInSeconds),
                        ifElse(useDefaultDurations, null, requestTimeOutInSeconds),
                        encrypt,
                        compress
                )
        )
    }

    @Test
    fun testProcessInvalidate() {
        trueFalseSequence { useRequestParameters ->
            testProcessAnnotation(
                    getAnnotation<Invalidate>(listOf(useRequestParameters)),
                    Operation.Local.Invalidate(useRequestParameters)
            )
        }
    }

    @Test
    fun testProcessDoNotCache() {
        testProcessAnnotation(
                getAnnotation<DoNotCache>(emptyList()),
                Remote.DoNotCache
        )
    }

    @Test
    fun testProcessClearTargetResponseClass() {
        trueFalseSequence { clearStaleEntriesOnly ->
            trueFalseSequence { useRequestParameters ->
                testProcessAnnotation(
                        getAnnotation<Clear>(listOf(
                                clearStaleEntriesOnly,
                                useRequestParameters
                        )),
                        Operation.Local.Clear(
                                clearStaleEntriesOnly,
                                useRequestParameters
                        )
                )
            }
        }
    }

    private fun testProcessAnnotation(annotation: Annotation,
                                      expectedOperation: Operation) {
        val actualOperation = target.process(
                arrayOf(annotation),
                OBSERVABLE,
                responseClass
        )!!

        assertOperation(
                expectedOperation,
                actualOperation,
                "Failed processing annotation $annotation"
        )
    }

}
