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

package dev.pthomain.android.dejavu.error

import java.io.IOException

/**
 * Default error class for DejaVu, replacing the previous Glitch class.
 * Implements NetworkErrorPredicate to indicate whether the error was network-related.
 *
 * @param cause the original throwable that caused this error
 * @param httpStatusCode the HTTP status code if available, or [NON_HTTP_STATUS]
 * @param errorCode a categorised error code
 * @param description an optional human-readable description
 */
class DejaVuError(
        override val cause: Throwable,
        val httpStatusCode: Int = NON_HTTP_STATUS,
        val errorCode: ErrorCode = ErrorCode.UNKNOWN,
        val description: String? = null
) : Exception(description ?: cause.message, cause),
        NetworkErrorPredicate {

    override val isNetworkError: Boolean
        get() = cause is IOException || errorCode == ErrorCode.NETWORK

    /**
     * Categorised error codes for common error scenarios.
     */
    enum ErrorCode {
        UNKNOWN,
        NETWORK,
        CONFIG,
        UNEXPECTED_RESPONSE
    }

    companion object {
        /** Sentinel value indicating the error did not originate from an HTTP response. */
        const val NON_HTTP_STATUS = -1
    }
}
