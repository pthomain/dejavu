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

package dev.pthomain.android.dejavu.interceptors.cache.metadata.token

import dev.pthomain.android.dejavu.interceptors.cache.instruction.CacheInstruction
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.Operation.Remote
import java.util.*

/**
 * Holds the request's instruction.
 *
 * @param instruction the original request cache instruction
 */
open class InstructionToken<O : Operation> internal constructor(
        val instruction: CacheInstruction<O>
) {

    inline fun <reified O : Operation> asInstruction() =
            cast<O, InstructionToken<O>>()

    inline fun <reified O : Operation> asStatus() =
            cast<O, StatusToken<O>>()

    inline fun <reified O : Operation> asRequest() =
            cast<O, RequestToken<O>>()

    inline fun <reified O : Remote> asResponse() =
            cast<O, ResponseToken<O>>()

    inline fun <reified O : Operation, reified T : InstructionToken<O>> cast(): T =
            if (instruction.operation is O && this is T) this
            else throw ClassCastException("Could not cast this token to ${T::class.java.name}")
}

/**
 * Holds the request's instruction and the response's status.
 *
 * @param instruction the original request cache instruction
 * @param status the cache status of the response
 */
sealed class StatusToken<O : Operation>(
        instruction: CacheInstruction<O>,
        val status: CacheStatus
) : InstructionToken<O>(instruction)

/**
 * Holds the request's instruction, the response's status and the
 * date at which the request was executed.
 *
 * This applies to operations of type Remote, i.e. returning data.
 * @see dev.pthomain.android.dejavu.interceptors.cache.instruction.Remote
 *
 * @param instruction the original request cache instruction
 * @param status the cache status of the response
 * @param requestDate the date at which the request was made
 */
open class RequestToken<O : Operation> internal constructor(
        instruction: CacheInstruction<O>,
        status: CacheStatus,
        val requestDate: Date
) : StatusToken<O>(instruction, status)

/**
 * Holds the request's instruction, the response's status, the
 * date at which the request was executed and optionally the date
 * at which the response was cached and if so its expiry date.
 *
 * This token is returned for responses returning data.
 *
 * @param instruction the original request cache instruction
 * @param status the cache status of the response
 * @param requestDate the date at which the request was made
 * @param cacheDate the optional date at which the response was cached
 * @param expiryDate the optional date at which the response will expire
 */
class ResponseToken<O : Remote> internal constructor(
        instruction: CacheInstruction<O>,
        status: CacheStatus,
        requestDate: Date,
        val cacheDate: Date? = null,
        val expiryDate: Date? = null
) : RequestToken<O>(instruction, status, requestDate)
