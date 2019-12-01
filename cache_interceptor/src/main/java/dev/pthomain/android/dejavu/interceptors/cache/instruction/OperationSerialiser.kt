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

package dev.pthomain.android.dejavu.interceptors.cache.instruction

import dev.pthomain.android.dejavu.configuration.DejaVuConfiguration.Companion.DEFAULT_CACHE_DURATION_IN_SECONDS
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation.*
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation.Type.*
import dev.pthomain.android.dejavu.utils.Utils.swapWhenDefault

/**
 * Class in charge of operating a simple serialisation of Operation
 */
internal class OperationSerialiser {

    private companion object {
        private const val SEPARATOR = ":"
    }

    /**
     * Serialises an operation with its associated arguments
     *
     * @param type the operation type to serialise
     * @param arguments all the associated operation's arguments
     * @return the serialised operation
     */
    fun serialise(type: Type,
                  vararg arguments: Any?) =
            arguments.joinToString(SEPARATOR) {
                when (it) {
                    null -> ""
                    -1L -> ""
                    is Class<*> -> it.name
                    is CachePriority -> it.name
                    else -> it.toString()
                }
            }.let {
                if (it.replace(":+", "").isBlank()) type.name
                else type.name + SEPARATOR + it
            }

    /**
     * Deserialises an operation with its associated arguments
     *
     * @param serialised the serialised operation as output by this class' serialise method
     * @return the deserialised cache instruction for the given serialised operation
     */
    fun deserialise(serialised: String) =
            serialised.split(SEPARATOR).let { params ->
                when (params[0]) {
                    DO_NOT_CACHE.name -> DoNotCache
                    INVALIDATE.name -> getInvalidateOperation(params)
                    CLEAR.name -> getClearOperation(params)
                    CACHE.name -> getCacheOperation(params)
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
                toBoolean(params[index]) ?: defaultValue
            else defaultValue

    /**
     * Converts a given serialised input to a nullable Long
     */
    private fun toInt(value: String,
                      defaultValue: Int? = null) =
            if (value == "") null
            else value.toInt().swapWhenDefault(defaultValue)

    /**
     * Returns a Clear operation instance for the given list of parameters
     */
    private fun getClearOperation(params: List<String>) = Clear(
            toBoolean(params, 1),
            toBoolean(params, 2)
    )

    /**
     * Returns a Invalidate operation instance for the given list of parameters
     */
    private fun getInvalidateOperation(params: List<String>) = Invalidate(
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
                        durationInSeconds,
                        connectivityTimeoutInSeconds,
                        requestTimeOutInSeconds,
                        encrypt,
                        compress
                )
            } else Cache()

}
