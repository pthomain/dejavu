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

package dev.pthomain.android.dejavu.shared.token.instruction.operation

import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Local.Clear
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Local.Invalidate
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Remote.Cache
import dev.pthomain.android.dejavu.shared.token.instruction.operation.Operation.Remote.DoNotCache
import dev.pthomain.android.dejavu.shared.utils.swapWhenDefault

/**
 * Class in charge of operating a simple serialisation of Operation
 */

private const val SEPARATOR = ":"

/**
 * Serialises an operation with its associated arguments
 *
 * @param arguments all the associated operation's arguments
 * @return the serialised operation
 */
internal fun Operation.serialise(vararg arguments: Any?): String {
    val operationName = this::class.java.simpleName

    return arguments.joinToString(SEPARATOR) {
        when (it) {
            null -> ""
            -1L -> ""
            is Class<*> -> it.name
            is CachePriority -> it.name
            else -> it.toString()
        }
    }.let {
        if (it.replace("$SEPARATOR+", "").isBlank()) operationName
        else "$operationName$SEPARATOR$it"
    }
}

/**
 * Deserialises an operation with its associated arguments
 *
 * @return the deserialised cache instruction for the given serialised operation
 */
fun String.toOperation() =
        split(SEPARATOR).let { params ->
            when (params[0]) {
                DoNotCache::class.java.simpleName -> DoNotCache
                Invalidate::class.java.simpleName -> Invalidate
                Clear::class.java.simpleName -> getClearOperation(params)
                Cache::class.java.simpleName -> getCacheOperation(params)
                else -> null
            }
        }

/**
 * Converts a given serialised input to a nullable Boolean
 */
private fun toBoolean(value: String) =
        if (value == "") null else value == "true"

/**
 * Converts a given serialised input to a nullable Boolean
 */
private fun toBoolean(params: List<String>,
                      index: Int,
                      defaultValue: Boolean = false) =
        if (params.size > index)
            toBoolean(params[index])
                    ?: defaultValue
        else defaultValue

/**
 * Converts a given serialised input to a nullable Long
 */
private fun toInt(value: String,
                  defaultValue: Int? = null) =
        if (value == "") defaultValue
        else value.toInt().swapWhenDefault(defaultValue)

/**
 * Returns a Clear operation instance for the given list of parameters
 */
private fun getClearOperation(params: List<String>) =
        Clear(
                toBoolean(params, 1)
        )

/**
 * Returns an Expiring operation instance for the given list of parameters
 */
private fun getCacheOperation(params: List<String>) =
        if (params.size == 7) {
            val priority = CachePriority.valueOf(params[1])
            val durationInSeconds = toInt(params[2], DEFAULT_CACHE_DURATION_IN_SECONDS)!!
            val connectivityTimeoutInSeconds = toInt(params[3])
            val requestTimeOutInSeconds = toInt(params[4])
            val encrypt = toBoolean(params, 5)
            val compress = toBoolean(params, 6)

            Cache(
                    priority,
                    durationInSeconds.swapWhenDefault(DEFAULT_CACHE_DURATION_IN_SECONDS)!!,
                    connectivityTimeoutInSeconds.swapWhenDefault(null),
                    requestTimeOutInSeconds.swapWhenDefault(null),
                    encrypt,
                    compress
            )
        } else Cache()

