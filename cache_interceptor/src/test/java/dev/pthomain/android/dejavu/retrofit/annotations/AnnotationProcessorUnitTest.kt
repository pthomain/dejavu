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

package dev.pthomain.android.dejavu.retrofit.annotations

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.boilerplate.core.utils.kotlin.ifElse
import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration
import dev.pthomain.android.dejavu.interceptors.RxType.OBSERVABLE
import dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority.DEFAULT
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.test.assertNullWithContext
import dev.pthomain.android.dejavu.test.assertOperation
import dev.pthomain.android.dejavu.test.expectException
import dev.pthomain.android.dejavu.test.getAnnotation
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import org.junit.Before
import org.junit.Test

class AnnotationProcessorUnitTest {

    private val responseKClass = TestResponse::class
    private val responseClass = responseKClass.java

    private lateinit var configuration: DejaVuConfiguration<Glitch>
    private lateinit var target: AnnotationProcessor<Glitch>

    @Before
    fun setUp() {
        configuration = mock()

        whenever(configuration.logger).thenReturn(mock())

        target = AnnotationProcessor(configuration)
    }

    @Test
    fun testProcessWithNoAnnotationsCacheAllByDefaultFalse() {
        testProcessWithNoAnnotations(false)
    }

    @Test
    fun testProcessWithNoAnnotationsCacheAllByDefaultTrue() {
        testProcessWithNoAnnotations(true)
    }

    private fun testProcessWithNoAnnotations(cacheAllByDefault: Boolean) {
        val operation = ifElse(cacheAllByDefault, Operation.Cache(), Operation.DoNotCache)
        whenever(configuration.cachePredicate).thenReturn { _ -> operation }

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
                + " TestResponse, found @Cache after existing annotation @DoNotCache."
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
    fun testProcessCacheDefaultDurations() {
        testProcessAnnotation(
                getAnnotation<Cache>(listOf(
                        DEFAULT,
                        -1,
                        -1,
                        -1,
                        true,
                        true
                )),
                Operation.Cache(
                        DEFAULT,
                        -1,
                        -1,
                        -1,
                        true,
                        true
                )
        )
    }

    @Test
    fun testProcessCacheDefaultNetworkTimeout() {
        testProcessAnnotation(
                getAnnotation<Cache>(listOf(
                        DEFAULT,
                        4567,
                        -1,
                        -1,
                        true,
                        true
                )),
                Operation.Cache(
                        DEFAULT,
                        4567,
                        -1,
                        -1,
                        true,
                        true
                )
        )
    }

    //TODO use a loop for this
    @Test
    fun testProcessCacheNoDefaultDurations() {
        testProcessAnnotation(
                getAnnotation<Cache>(listOf(
                        DEFAULT,
                        4567,
                        5678,
                        -1,
                        true,
                        true
                )),
                Operation.Cache(
                        DEFAULT,
                        4567,
                        5678,
                        -1,
                        true,
                        true
                )
        )
    }

    @Test
    fun testProcessInvalidate() {
        testProcessAnnotation(
                getAnnotation<Invalidate>(listOf(responseKClass)),
                Operation.Invalidate()
        )
    }

    @Test
    fun testProcessDoNotCache() {
        testProcessAnnotation(
                getAnnotation<DoNotCache>(listOf()),
                Operation.DoNotCache
        )
    }

    @Test
    fun testProcessClearTargetResponseClass() {
        testProcessAnnotation(
                getAnnotation<Clear>(listOf(
                        responseKClass,
                        false
                )),
                Operation.Clear()
        )
    }

    @Test
    fun testProcessClearTargetResponseClassOlderEntries() {
        testProcessAnnotation(
                getAnnotation<Clear>(listOf(
                        responseKClass,
                        true
                )),
                Operation.Clear(
                        clearStaleEntriesOnly = true
                )
        )
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
