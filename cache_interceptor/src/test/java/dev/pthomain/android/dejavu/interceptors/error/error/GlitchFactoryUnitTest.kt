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

package dev.pthomain.android.dejavu.interceptors.error.error

import com.google.gson.stream.MalformedJsonException
import com.nhaarman.mockitokotlin2.whenever
import dev.pthomain.android.dejavu.interceptors.error.GlitchFactory
import dev.pthomain.android.dejavu.interceptors.error.glitch.ErrorCode
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import retrofit2.HttpException
import java.io.IOException

class GlitchFactoryUnitTest {

    private lateinit var target: GlitchFactory

    @Before
    @Throws(Exception::class)
    fun setUp() {
        target = GlitchFactory()
    }

    @Test
    fun testParseMalformedJsonException() {
        val exception = MalformedJsonException("malformed")

        val apiError = target(exception)

        assertGlitch(
                apiError,
                "malformed",
                ErrorCode.NETWORK,
                -1,
                true
        )
    }

    @Test
    fun testParseIOException() {
        val exception = IOException("time out")

        val apiError = target(exception)

        assertGlitch(
                apiError,
                "time out",
                ErrorCode.NETWORK,
                -1,
                true
        )
    }

    @Test
    fun testParseHttpExceptionUnauthorised() {
        val exception = mock(HttpException::class.java)
        whenever(exception.code()).thenReturn(401)
        whenever(exception.message()).thenReturn("not authorised")

        val apiError = target(exception)

        assertGlitch(
                apiError,
                "not authorised",
                ErrorCode.UNAUTHORISED,
                401,
                false
        )
    }

    @Test
    fun testParseHttpExceptionServerError() {
        val exception = mock(HttpException::class.java)
        whenever(exception.code()).thenReturn(500)
        whenever(exception.message()).thenReturn("server error")

        val apiError = target(exception)

        assertGlitch(
                apiError,
                "server error",
                ErrorCode.SERVER_ERROR,
                500,
                false
        )
    }

    @Test
    fun testParseHttpExceptionUnknown() {
        val exception = mock(HttpException::class.java)
        whenever(exception.code()).thenReturn(1)
        whenever(exception.message()).thenReturn("unknown")

        val apiError = target(exception)

        assertGlitch(
                apiError,
                "unknown",
                ErrorCode.UNKNOWN,
                1,
                false
        )
    }

    companion object {

        fun assertGlitch(glitch: Glitch,
                         expectedRawDescription: String?,
                         expectedErrorCode: ErrorCode,
                         expectedHttpCode: Int,
                         isNetworkError: Boolean) {
            assertNotNull("glitch should not be null", glitch)

            val rawDescription = glitch.description

            val httpStatus = glitch.httpStatus
            assertEquals("httpStatus should be $expectedHttpCode", expectedHttpCode, httpStatus)

            if (httpStatus == 200) {
                if (expectedRawDescription == null) {
                    assertNull("rawDescription should be null, was: " + rawDescription!!, rawDescription)
                } else {
                    assertNotNull("rawDescription should not be null", rawDescription)
                    assertTrue("rawDescription should contain message: $expectedRawDescription",
                            rawDescription!!.contains(expectedRawDescription)
                    )
                }
            }

            assertEquals("Expected network glitch == $isNetworkError",
                    isNetworkError,
                    glitch.isNetworkError()
            )

            val errorCode = glitch.errorCode
            assertNotNull("errorCode should not be null", errorCode)
            assertEquals("errorCode should be $expectedErrorCode", expectedErrorCode, errorCode)
        }
    }
}