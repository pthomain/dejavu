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

import dev.pthomain.android.dejavu.cache.CacheException
import dev.pthomain.android.dejavu.error.DejaVuError.ErrorCode
import dev.pthomain.android.dejavu.error.DejaVuError.Companion.NON_HTTP_STATUS
import java.io.IOException

/**
 * Default implementation of ErrorFactory for DejaVu, replacing DejaVuGlitchFactory.
 * Converts throwables into DejaVuError instances with appropriate error codes.
 */
class DejaVuErrorFactory : ErrorFactory<DejaVuError> {

    override fun invoke(throwable: Throwable): DejaVuError =
            when {
                throwable is DejaVuError -> throwable

                throwable is CacheException -> DejaVuError(
                        throwable,
                        NON_HTTP_STATUS,
                        ErrorCode.CONFIG,
                        "Configuration error"
                )

                throwable is IOException -> DejaVuError(
                        throwable,
                        NON_HTTP_STATUS,
                        ErrorCode.NETWORK,
                        throwable.message
                )

                else -> DejaVuError(
                        throwable,
                        NON_HTTP_STATUS,
                        ErrorCode.UNKNOWN,
                        throwable.message
                )
            }
}
