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

package dev.pthomain.android.dejavu.configuration.instruction

import dev.pthomain.android.dejavu.configuration.instruction.Operation.*
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Expiring.*
import dev.pthomain.android.dejavu.configuration.instruction.Operation.Type.*

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
    fun serialise(type: Type?,
                  vararg arguments: Any?) =
            arguments.joinToString(separator = SEPARATOR) {
                when (it) {
                    null -> ""
                    is Class<*> -> it.name
                    else -> it.toString()
                }
            }.let {
                when {
                    type == null -> it
                    it.replace(":+", "").isBlank() -> type.name
                    else -> type.name + SEPARATOR + it
                }
            }

    /**
     * Deserialises an operation with its associated arguments
     *
     * @param serialised the serialised operation as output by this class' serialise method
     * @return the deserialised cache instruction for the given serialised operation
     */
    fun deserialise(serialised: String) =
            serialised.split(SEPARATOR).let { params ->
                try {
                    when (params[0]) {
                        DO_NOT_CACHE.name -> DoNotCache

                        INVALIDATE.name -> getInvalidateOperation(params)

                        CLEAR.name -> getClearOperation(params)

                        CACHE.name,
                        REFRESH.name,
                        OFFLINE.name -> getExpiringOperation(params)

                        else -> null
                    }
                } catch (e: Exception) {
                    null
                }
            }

    /**
     * Returns a class for the given name or null, without throwing an exception
     */
    private fun getClassForName(value: String): Class<*>? = try {
        Class.forName(value)
    } catch (e: Exception) {
        null
    }

    /**
     * Converts a given serialised input to a nullable Boolean
     */
    private fun toBoolean(value: String) =
            if (value == "") null else value == "true"

    /**
     * Converts a given serialised input to a nullable Long
     */
    private fun toLong(value: String) =
            if (value == "") null else value.toLong()

    /**
     * Returns a Clear operation instance for the given list of parameters
     */
    private fun getClearOperation(params: List<String>) =
            if (params.size == 3) {
                Clear(
                        getClassForName(params[1]),
                        toBoolean(params[2]) ?: false
                )
            } else Clear()

    /**
     * Returns a Invalidate operation instance for the given list of parameters
     */
    private fun getInvalidateOperation(params: List<String>) =
            if (params.size == 2) {
                Invalidate(getClassForName(params[1]))
            } else Invalidate()

    /**
     * Returns an Expiring operation instance for the given list of parameters
     */
    private fun getExpiringOperation(params: List<String>) =
            if (params.size == 8) {
                val durationInMillis = toLong(params[1])
                val connectivityTimeoutInMillis = toLong(params[2])
                val freshOnly = toBoolean(params[3]) ?: false
                val mergeOnNextOnError = toBoolean(params[4])
                val encrypt = toBoolean(params[5])
                val compress = toBoolean(params[6])
                val filterFinal = toBoolean(params[7]) ?: false

                when (params[0]) {
                    CACHE.name -> Cache(
                            durationInMillis,
                            connectivityTimeoutInMillis,
                            freshOnly,
                            mergeOnNextOnError,
                            encrypt,
                            compress,
                            filterFinal
                    )
                    REFRESH.name -> Refresh(
                            durationInMillis,
                            connectivityTimeoutInMillis,
                            freshOnly,
                            mergeOnNextOnError,
                            filterFinal
                    )
                    OFFLINE.name -> Offline(
                            freshOnly,
                            mergeOnNextOnError
                    )
                    else -> throw IllegalArgumentException("Unknown type")
                }
            } else when (params[0]) {
                CACHE.name -> Cache()
                REFRESH.name -> Refresh()
                OFFLINE.name -> Offline()
                else -> throw IllegalArgumentException("Unknown type")
            }

}
