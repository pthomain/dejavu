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

package uk.co.glass_software.android.cache_interceptor.interceptors.internal.error

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import retrofit2.HttpException
import uk.co.glass_software.android.cache_interceptor.configuration.ErrorFactory
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ApiError.Companion.NON_HTTP_STATUS
import uk.co.glass_software.android.cache_interceptor.interceptors.internal.error.ErrorCode.*
import java.io.IOException
import java.util.concurrent.TimeoutException

class ApiErrorFactory : ErrorFactory<ApiError> {

    override fun getError(throwable: Throwable) =
            when (throwable) {
                is IOException,
                is JsonParseException,
                is TimeoutException -> getIoError(throwable)

                is HttpException -> getHttpError(throwable)

                else -> getDefaultError(throwable)
            }

    private fun getHttpError(throwable: HttpException) =
            ApiError(
                    throwable,
                    throwable.code(),
                    parseErrorCode(throwable),
                    throwable.message()
            )

    private fun getIoError(throwable: Throwable) =
            ApiError(
                    throwable,
                    NON_HTTP_STATUS,
                    if (throwable is MalformedJsonException || throwable is JsonParseException) UNEXPECTED_RESPONSE else NETWORK,
                    throwable.message
            )

    private fun getDefaultError(throwable: Throwable) =
            ApiError.from(throwable) ?: ApiError(
                    throwable,
                    NON_HTTP_STATUS,
                    UNKNOWN,
                    "${throwable.javaClass.name}: ${throwable.message}"
            )

    private fun parseErrorCode(httpException: HttpException) =
            when (httpException.code()) {
                401 -> UNAUTHORISED
                404 -> NOT_FOUND
                500 -> SERVER_ERROR
                else -> UNKNOWN
            }
}
