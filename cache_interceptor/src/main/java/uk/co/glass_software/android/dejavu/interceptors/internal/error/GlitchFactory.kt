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

import retrofit2.HttpException
import uk.co.glass_software.android.dejavu.configuration.ErrorFactory
import uk.co.glass_software.android.dejavu.interceptors.internal.error.ErrorCode.*
import uk.co.glass_software.android.dejavu.interceptors.internal.error.Glitch.Companion.NON_HTTP_STATUS
import uk.co.glass_software.android.dejavu.retrofit.annotations.CacheException
import java.io.IOException
import java.util.concurrent.TimeoutException

//TODO JavaDoc
open class GlitchFactory : ErrorFactory<Glitch> {

    override fun getError(throwable: Throwable) =
            when (throwable) {
                is IOException,
                is TimeoutException -> getIoError(throwable)

                is HttpException -> getHttpError(throwable)
                is CacheException -> getConfigError(throwable)

                else -> getDefaultError(throwable)
            }

    private fun getHttpError(throwable: HttpException) =
            Glitch(
                    throwable,
                    throwable.code(),
                    parseErrorCode(throwable),
                    throwable.message()
            )

    private fun getConfigError(throwable: CacheException) =
            Glitch(
                    throwable,
                    NON_HTTP_STATUS,
                    CONFIG,
                    "Configuration error"
            )

    private fun getIoError(throwable: Throwable) =
            Glitch(
                    throwable,
                    NON_HTTP_STATUS,
                    NETWORK,
                    throwable.message
            )

    private fun getDefaultError(throwable: Throwable) =
            Glitch.from(throwable) ?: Glitch(
                    throwable,
                    NON_HTTP_STATUS,
                    UNKNOWN
            )

    private fun parseErrorCode(httpException: HttpException) =
            when (httpException.code()) {
                401 -> UNAUTHORISED
                404 -> NOT_FOUND
                500 -> SERVER_ERROR
                else -> UNKNOWN
            }
}
