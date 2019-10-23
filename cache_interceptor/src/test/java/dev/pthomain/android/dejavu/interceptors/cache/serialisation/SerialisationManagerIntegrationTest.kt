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

package dev.pthomain.android.dejavu.interceptors.cache.serialisation

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import dev.pthomain.android.boilerplate.core.utils.lambda.Action
import dev.pthomain.android.dejavu.configuration.instruction.CacheInstruction.Operation.Expiring.Cache
import dev.pthomain.android.dejavu.interceptors.cache.metadata.token.CacheToken
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.SerialisationManager.Factory.Type.FILE
import dev.pthomain.android.dejavu.interceptors.cache.serialisation.decoration.SerialisationDecorationMetadata
import dev.pthomain.android.dejavu.interceptors.error.ResponseWrapper
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import dev.pthomain.android.dejavu.test.BaseIntegrationTest
import dev.pthomain.android.dejavu.test.assertResponseWrapperWithContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

internal class SerialisationManagerIntegrationTest
    : BaseIntegrationTest<SerialisationManager<Glitch>>({
    it.serialisationManagerFactory().create(FILE) //TODO test the factory
}) {

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

    //FIXME use cases

    @Test
    @Throws(Exception::class)
    fun testCompress() {
        val compressed = target.serialise(
                wrapper,
                SerialisationDecorationMetadata(true, false)
        )

        assertEquals(
                "Wrong compressed size",
                2566,
                compressed.size
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUncompressSuccess() {
        val compressed = target.serialise(
                wrapper,
                SerialisationDecorationMetadata(true, false)
        )

        val uncompressed = target.deserialise(
                instructionToken,
                compressed,
                SerialisationDecorationMetadata(true, false)
        )

        assertResponseWrapperWithContext(
                wrapper,
                uncompressed,
                "Response wrapper didn't match"
        )

        verify(mockErrorCallback, never()).invoke()
    }

    @Test
    @Throws(Exception::class)
    fun testUncompressFailure() {
        val compressed = target.serialise(
                wrapper,
                SerialisationDecorationMetadata(true, false)
        )

        for (i in 0..49) {
            compressed[i] = 0
        }

        target.deserialise(
                instructionToken,
                compressed,
                SerialisationDecorationMetadata(true, false)
        )

        verify(mockErrorCallback).invoke()
    }

    //TODO test encryption
}