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

package uk.co.glass_software.android.dejavu.interceptors.internal.error

import com.google.gson.stream.MalformedJsonException
import junit.framework.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import retrofit2.HttpException
import java.io.IOException

class ApiErrorFactoryUnitTest {

    private lateinit var target: ApiErrorFactory

    @Before
    @Throws(Exception::class)
    fun setUp() {
        target = ApiErrorFactory()
    }

    @Test
    fun testParseMalformedJsonException() {
        val exception = MalformedJsonException("malformed")

        val apiError = target.getError(exception)

        assertApiError(
                apiError,
                "malformed",
                ErrorCode.UNEXPECTED_RESPONSE,
                -1,
                false
        )
    }

    @Test
    fun testParseIOException() {
        val exception = IOException("time out")

        val apiError = target.getError(exception)

        assertApiError(
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
        `when`(exception.code()).thenReturn(401)
        `when`(exception.message()).thenReturn("not authorised")

        val apiError = target.getError(exception)

        assertApiError(
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
        `when`(exception.code()).thenReturn(500)
        `when`(exception.message()).thenReturn("server error")

        val apiError = target.getError(exception)

        assertApiError(
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
        `when`(exception.code()).thenReturn(1)
        `when`(exception.message()).thenReturn("unknown")

        val apiError = target.getError(exception)

        assertApiError(
                apiError,
                "unknown",
                ErrorCode.UNKNOWN,
                1,
                false
        )
    }

    companion object {

        fun assertApiError(error: ApiError,
                           expectedRawDescription: String?,
                           expectedErrorCode: ErrorCode,
                           expectedHttpCode: Int,
                           isNetworkError: Boolean) {
            assertNotNull("error should not be null", error)

            val rawDescription = error.description

            val httpStatus = error.httpStatus
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

            assertEquals("Expected network error == $isNetworkError",
                    isNetworkError,
                    error.isNetworkError()
            )

            val errorCode = error.errorCode
            assertNotNull("errorCode should not be null", errorCode)
            assertEquals("errorCode should be $expectedErrorCode", expectedErrorCode, errorCode)
        }
    }
}