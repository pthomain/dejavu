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
import dev.pthomain.android.dejavu.configuration.CacheConfiguration
import dev.pthomain.android.dejavu.configuration.CacheInstruction
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Type.DO_NOT_CACHE
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Type.OFFLINE
import dev.pthomain.android.dejavu.interceptors.internal.error.Glitch
import dev.pthomain.android.dejavu.retrofit.annotations.AnnotationProcessor.RxType.OBSERVABLE
import dev.pthomain.android.dejavu.test.*
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import org.junit.Before
import org.junit.Test

class AnnotationProcessorUnitTest {

    private val defaultCacheDuration = 4321L
    private val defaultNetworkTimeOut = 1234L
    private val responseKClass = TestResponse::class
    private val responseClass = responseKClass.java

    private lateinit var configuration: CacheConfiguration<Glitch>
    private lateinit var target: AnnotationProcessor<Glitch>

    @Before
    fun setUp() {
        configuration = mock()

        whenever(configuration.logger).thenReturn(mock())
        whenever(configuration.cachePredicate).thenReturn({ _, _ -> false })
        whenever(configuration.cacheDurationInMillis).thenReturn(defaultCacheDuration)
        whenever(configuration.connectivityTimeoutInMillis).thenReturn(defaultNetworkTimeOut)
        whenever(configuration.mergeOnNextOnError).thenReturn(true)
        whenever(configuration.encrypt).thenReturn(true)
        whenever(configuration.compress).thenReturn(true)

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
        whenever(configuration.cachePredicate).thenReturn({ _, _ -> cacheAllByDefault })

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
                + " ${OBSERVABLE.getTypedName(responseClass)}, found ${OFFLINE.annotationName}"
                + " after existing annotation ${DO_NOT_CACHE.annotationName}."
                + " Only one annotation can be used for this method.")

        expectException(
                CacheException::class.java,
                expectedErrorMessage,
                {
                    target.process(
                            arrayOf(
                                    getAnnotation<DoNotCache>(emptyList()),
                                    getAnnotation<Offline>(emptyList())
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
                        true,
                        -1L,
                        -1L,
                        OptionalBoolean.TRUE,
                        OptionalBoolean.TRUE,
                        OptionalBoolean.TRUE
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Cache(
                                defaultCacheDuration,
                                defaultNetworkTimeOut,
                                true,
                                true,
                                true,
                                true,
                                false
                        )
                )
        )
    }

    @Test
    fun testProcessCacheDefaultNetworkTimeout() {
        testProcessAnnotation(
                getAnnotation<Cache>(listOf(
                        true,
                        4567L,
                        -1L,
                        OptionalBoolean.TRUE,
                        OptionalBoolean.TRUE,
                        OptionalBoolean.TRUE
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Cache(
                                4567L,
                                defaultNetworkTimeOut,
                                true,
                                true,
                                true,
                                true,
                                false
                        )
                )
        )
    }

    @Test
    fun testProcessCacheNoDefaultDurations() {
        testProcessAnnotation(
                getAnnotation<Cache>(listOf(
                        true,
                        4567L,
                        5678L,
                        OptionalBoolean.TRUE,
                        OptionalBoolean.TRUE,
                        OptionalBoolean.TRUE
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Cache(
                                4567L,
                                5678L,
                                true,
                                true,
                                true,
                                true,
                                false
                        )
                )
        )
    }

    @Test
    fun testProcessRefreshDefaultDurations() {
        testProcessAnnotation(
                getAnnotation<Refresh>(listOf(
                        true,
                        -1L,
                        -1L,
                        OptionalBoolean.TRUE
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Refresh(
                                defaultCacheDuration,
                                defaultNetworkTimeOut,
                                true,
                                true,
                                false
                        )
                )
        )
    }

    @Test
    fun testProcessRefreshDefaultNetworkTimeout() {
        testProcessAnnotation(
                getAnnotation<Refresh>(listOf(
                        true,
                        4567L,
                        -1L,
                        OptionalBoolean.TRUE
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Refresh(
                                4567L,
                                defaultNetworkTimeOut,
                                true,
                                true,
                                false
                        )
                )
        )
    }

    @Test
    fun testProcessRefreshNoDefaultDurations() {
        testProcessAnnotation(
                getAnnotation<Refresh>(listOf(
                        true,
                        4567L,
                        5678L,
                        OptionalBoolean.TRUE
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Refresh(
                                4567L,
                                5678L,
                                true,
                                true,
                                false
                        )
                )
        )
    }

    @Test
    fun testProcessOffline() {
        testProcessAnnotation(
                getAnnotation<Offline>(listOf()),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Offline()
                )
        )
    }

    @Test
    fun testProcessOfflineWithArgs() {
        testProcessAnnotation(
                getAnnotation<Offline>(listOf(
                        true,
                        OptionalBoolean.TRUE
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Expiring.Offline(
                                true,
                                true
                        )
                )
        )
    }

    @Test
    fun testProcessInvalidate() {
        testProcessAnnotation(
                getAnnotation<Invalidate>(listOf(responseKClass)),
                cacheInstruction(
                        CacheInstruction.Operation.Invalidate
                )
        )
    }

    @Test
    fun testProcessDoNotCache() {
        testProcessAnnotation(
                getAnnotation<DoNotCache>(listOf()),
                cacheInstruction(
                        CacheInstruction.Operation.DoNotCache
                )
        )
    }

    @Test
    fun testProcessClearTargetResponseClass() {
        testProcessAnnotation(
                getAnnotation<Clear>(listOf(
                        responseKClass,
                        false
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Clear(responseClass)
                )
        )
    }

    @Test
    fun testProcessClearTargetResponseClassOlderEntries() {
        testProcessAnnotation(
                getAnnotation<Clear>(listOf(
                        responseKClass,
                        true
                )),
                cacheInstruction(
                        CacheInstruction.Operation.Clear(
                                responseClass,
                                true
                        )
                )
        )
    }

    @Test
    fun testProcessClearAll() {
        testProcessAnnotation(
                getAnnotation<ClearAll>(listOf(false)),
                cacheInstruction(
                        CacheInstruction.Operation.Clear(
                                null,
                                false
                        )
                )
        )
    }

    @Test
    fun testProcessClearAllOlderEntries() {
        testProcessAnnotation(
                getAnnotation<ClearAll>(listOf(true)),
                cacheInstruction(
                        CacheInstruction.Operation.Clear(
                                null,
                                true
                        )
                )
        )
    }

    private fun testProcessAnnotation(annotation: Annotation,
                                      expectedInstruction: CacheInstruction) {
        val actualInstruction = target.process(
                arrayOf(annotation),
                OBSERVABLE,
                responseClass
        )

        assertInstruction(
                expectedInstruction,
                actualInstruction,
                "Failed processing annotation $annotation"
        )
    }

}
