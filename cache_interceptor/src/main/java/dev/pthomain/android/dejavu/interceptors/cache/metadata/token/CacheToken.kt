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

package dev.pthomain.android.dejavu.interceptors.cache.metadata.token

import dev.pthomain.android.dejavu.interceptors.cache.instruction.CacheInstruction
import java.util.*

/**
 * Holds the request's instruction.
 *
 * @param instruction the original request cache instruction
 */
open class CacheToken internal constructor(open val instruction: CacheInstruction)

/**
 * Holds the request's instruction and the response's status.
 *
 * @param instruction the original request cache instruction
 * @param status the cache status of the response
 */
sealed class StatusToken(
        override val instruction: CacheInstruction,
        open val status: CacheStatus
) : CacheToken(instruction)

/**
 * Holds the request's instruction, the response's status and the
 * date at which the request was executed.
 *
 * This applies to operations of type Remote, i.e. returning data.
 * @see dev.pthomain.android.dejavu.interceptors.cache.instruction.Remote
 *
 * @param instruction the original request cache instruction
 * @param status the cache status of the response
 * @param fetchDate the date at which the request was made
 */
sealed class RemoteToken(
        override val instruction: CacheInstruction,
        override val status: CacheStatus,
        open val fetchDate: Date
) : StatusToken(instruction, status)

/**
 * Holds the request's instruction, the response's status and the
 * date at which the request was executed.
 *
 * This applies to operations of type Operation.Local, i.e. operating
 * solely on the local cache.
 * @see dev.pthomain.android.dejavu.interceptors.cache.instruction.Local
 *
 * @param instruction the original request cache instruction
 * @param status the cache status of the response
 * @param executionDate the date at which the operation was executed
 */
sealed class LocalToken(
        override val instruction: CacheInstruction,
        override val status: CacheStatus,
        open val executionDate: Date
) : StatusToken(instruction, status)

/**
 * Holds the request's instruction, the response's status, the
 * date at which the request was executed and optionally the date
 * at which the response was cached and if so its expiry date.
 *
 * This token is returned for responses returning data.
 *
 * @param instruction the original request cache instruction
 * @param status the cache status of the response
 * @param fetchDate the date at which the request was made
 * @param cacheDate the optional date at which the response was cached
 * @param expiryDate the optional date at which the response will expire
 */
data class ResponseToken internal constructor(
        override val instruction: CacheInstruction,
        override val status: CacheStatus,
        override val fetchDate: Date,
        val cacheDate: Date? = null,
        val expiryDate: Date? = null
) : RemoteToken(instruction, status, fetchDate)

/**
 * Holds the request's instruction, the response's status, the
 * date at which the request was executed.
 *
 * This is returned if the remote operation returned no response
 * due to filtering defined in the associated CachePriority.
 * @see dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority
 *
 * @param instruction the original request cache instruction
 * @param status the cache status of the response
 * @param fetchDate the date at which the request was made
 */
data class NetworkRemoteToken internal constructor(
        override val instruction: CacheInstruction,
        override val status: CacheStatus,
        override val fetchDate: Date
) : RemoteToken(instruction, status, fetchDate)

/**
 * Holds the request's instruction, the response's status, the
 * date at which the request was executed.
 *
 * This is returned if the remote operation returned no response
 * due to filtering defined in the associated CachePriority.
 * @see dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority
 *
 * @param instruction the original request cache instruction
 * @param status the cache status of the response
 * @param fetchDate the date at which the request was made
 */
data class EmptyRemoteToken internal constructor(
        override val instruction: CacheInstruction,
        override val status: CacheStatus,
        override val fetchDate: Date
) : RemoteToken(instruction, status, fetchDate)

/**
 * Holds the request's instruction, the response's status, the
 * date at which the request was executed.
 *
 * This is returned if the remote operation returned no response
 * due to some error.
 *
 * @param instruction the original request cache instruction
 * @param status the cache status of the response
 * @param fetchDate the date at which the request was made
 */
data class ErrorRemoteToken internal constructor(
        override val instruction: CacheInstruction,
        override val status: CacheStatus,
        override val fetchDate: Date
) : RemoteToken(instruction, status, fetchDate)

/**
 * Holds the request's instruction, the response's status, the
 * date at which the request was executed.
 *
 * This is returned when a local operation has been successfully executed.
 *
 * @param instruction the original request cache instruction
 * @param status the cache status of the response
 * @param executionDate the date at which the operation was executed
 */
data class EmptyLocalToken internal constructor(
        override val instruction: CacheInstruction,
        override val status: CacheStatus,
        override val executionDate: Date
) : LocalToken(instruction, status, executionDate)

/**
 * Holds the request's instruction, the response's status, the
 * date at which the request was executed.
 *
 * This is returned when a local operation's execution failed.
 *
 * @param instruction the original request cache instruction
 * @param status the cache status of the response
 * @param executionDate the date at which the operation was executed
 */
data class ErrorLocalToken internal constructor(
        override val instruction: CacheInstruction,
        override val status: CacheStatus,
        override val executionDate: Date
) : LocalToken(instruction, status, executionDate)

