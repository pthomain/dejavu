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

package dev.pthomain.android.dejavu.interceptors.error.glitch

import dev.pthomain.android.dejavu.interceptors.error.error.ErrorFactory
import dev.pthomain.android.dejavu.interceptors.error.glitch.ErrorCode.*
import dev.pthomain.android.dejavu.interceptors.error.glitch.Glitch.Companion.NON_HTTP_STATUS
import dev.pthomain.android.dejavu.retrofit.annotations.CacheException
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * Default implementation of ErrorFactory handling some usual base exceptions.
 *
 * @see dev.pthomain.android.dejavu.configuration.DejaVuConfiguration.errorFactory for overriding this factory
 * @see Glitch
 */
open class GlitchFactory : ErrorFactory<Glitch> {

    override val exceptionClass = Glitch::class.java

    /**
     * Converts a throwable to a Glitch, containing some metadata around the exception
     *
     * @param throwable the given throwable to make sense of
     * @return an instance of Glitch
     */
    override fun invoke(throwable: Throwable) =
            when (throwable) {
                is IOException,
                is TimeoutException -> getIoError(throwable)

                is HttpException -> getHttpError(throwable)
                is CacheException -> getConfigError(throwable)

                else -> getDefaultError(throwable)
            }

    /**
     * Converts an HttpException to a Glitch
     *
     * @param throwable the original exception
     * @return the converted Glitch
     */
    private fun getHttpError(throwable: HttpException) =
            Glitch(
                    throwable,
                    throwable.code(),
                    parseErrorCode(throwable),
                    throwable.message()
            )

    /**
     * Converts an CacheException to a Glitch
     *
     * @param throwable the original exception
     * @return the converted Glitch
     */
    private fun getConfigError(throwable: CacheException) =
            Glitch(
                    throwable,
                    NON_HTTP_STATUS,
                    CONFIG,
                    "Configuration error"
            )

    /**
     * Converts an IO exception to a Glitch
     *
     * @param throwable the original exception
     * @return the converted Glitch
     */
    private fun getIoError(throwable: Throwable) =
            Glitch(
                    throwable,
                    NON_HTTP_STATUS,
                    NETWORK,
                    throwable.message
            )

    /**
     * Converts a generic Exception to a Glitch
     *
     * @param throwable the original exception
     * @return the converted Glitch
     */
    private fun getDefaultError(throwable: Throwable) =
            Glitch.from(throwable)
                    ?: Glitch(
                            throwable,
                            NON_HTTP_STATUS,
                            UNKNOWN
                    )

    /**
     * Parses an HttpException and returns an associated ErrorCode.
     *
     * @param httpException the original exception
     * @return the associated ErrorCode
     */
    private fun parseErrorCode(httpException: HttpException) =
            when (httpException.code()) {
                401 -> UNAUTHORISED
                404 -> NOT_FOUND
                500 -> SERVER_ERROR
                else -> UNKNOWN
            }

}
