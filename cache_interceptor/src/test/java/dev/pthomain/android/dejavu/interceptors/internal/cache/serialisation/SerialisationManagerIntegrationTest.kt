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

package dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import dev.pthomain.android.boilerplate.core.utils.lambda.Action
import dev.pthomain.android.dejavu.configuration.CacheInstruction.Operation.Expiring.Cache
import dev.pthomain.android.dejavu.injection.integration.component.IntegrationCacheComponent
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.internal.error.Glitch
import dev.pthomain.android.dejavu.response.ResponseWrapper
import dev.pthomain.android.dejavu.test.BaseIntegrationTest
import dev.pthomain.android.dejavu.test.assertResponseWrapperWithContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class SerialisationManagerIntegrationTest
    : BaseIntegrationTest<SerialisationManager<Glitch>>(IntegrationCacheComponent::serialisationManager) {

    private lateinit var wrapper: ResponseWrapper<Glitch>
    private lateinit var instructionToken: CacheToken
    private lateinit var mockErrorCallback: Action

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        instructionToken = instructionToken(Cache())
        mockErrorCallback = mock()

        wrapper = getStubbedTestResponse(instructionToken)
    }

    @Test
    @Throws(Exception::class)
    fun testCompress() {
        val compressed = target.serialise(
                wrapper,
                false,
                true
        )

        assertEquals(
                "Wrong compressed size",
                2566,
                compressed!!.size
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUncompressSuccess() {
        val compressed = target.serialise(
                wrapper,
                false,
                true
        )!!

        val uncompressed = target.deserialise(
                instructionToken,
                compressed,
                false,
                true,
                mockErrorCallback
        )

        assertResponseWrapperWithContext(
                wrapper,
                uncompressed!!,
                "Response wrapper didn't match"
        )

        verify(mockErrorCallback, never()).invoke()
    }

    @Test
    @Throws(Exception::class)
    fun testUncompressFailure() {
        val compressed = target.serialise(
                wrapper,
                false,
                true
        )!!

        for (i in 0..49) {
            compressed[i] = 0
        }

        target.deserialise(
                instructionToken,
                compressed,
                false,
                true,
                mockErrorCallback
        )

        verify(mockErrorCallback).invoke()
    }
}