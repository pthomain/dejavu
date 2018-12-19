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

import uk.co.glass_software.android.dejavu.configuration.NetworkErrorProvider
import uk.co.glass_software.android.dejavu.interceptors.internal.error.ErrorCode.UNKNOWN

data class ApiError constructor(private val throwable: Throwable,
                                val httpStatus: Int = NON_HTTP_STATUS,
                                val errorCode: ErrorCode = UNKNOWN,
                                val description: String? = null)
    : Exception(throwable),
        NetworkErrorProvider {

    override fun isNetworkError() = errorCode === ErrorCode.NETWORK

    companion object {
        const val NON_HTTP_STATUS = -1
        fun from(throwable: Throwable) = throwable as? ApiError
    }
}