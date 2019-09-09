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

package uk.co.glass_software.android.dejavu.configuration

import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.*
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Expiring.*
import uk.co.glass_software.android.dejavu.configuration.CacheInstruction.Operation.Type.*

/**
 * Class in charge of operating a simple serialisation of CacheInstruction
 */
internal class CacheInstructionSerialiser {

    private companion object {
        private const val separator = ":"
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
            arguments.joinToString(separator = separator) {
                when (it) {
                    is Class<*> -> it.name
                    else -> it.toString()
                }
            }.let {
                if (type == null) it
                else type.name + separator + it
            }

    /**
     * Deserialises an operation with its associated arguments
     *
     * @param serialised the serialised operation as output by this class' serialise method
     * @return the deserialised cache instruction for the given serialised operation
     */
    fun deserialise(serialised: String) =
            if (serialised.contains(separator)) {
                serialised.split(separator).let { params ->
                    if (params.size >= 3) {
                        try {
                            when (params[1]) {
                                DO_NOT_CACHE.name -> DoNotCache
                                INVALIDATE.name -> Invalidate
                                CLEAR.name,
                                CLEAR_ALL.name -> getClearOperation(params)
                                CACHE.name,
                                REFRESH.name,
                                OFFLINE.name -> getExpiringOperation(params)
                                else -> null
                            }?.let { operation ->
                                getClassForName(params[0])?.let { responseClass ->
                                    CacheInstruction(responseClass, operation)
                                }
                            }
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                }
            } else null

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
            if (value == "null") null else value == "true"

    /**
     * Converts a given serialised input to a nullable Long
     */
    private fun toLong(value: String) =
            if (value == "null") null else value.toLong()

    /**
     * Returns a Clear operation instance for the given list of parameters
     */
    private fun getClearOperation(params: List<String>) =
            if (params.size == 4) {
                Clear(getClassForName(params[2]), toBoolean(params[3]) ?: false)
            } else null

    /**
     * Returns an Expiring operation instance for the given list of parameters
     */
    private fun getExpiringOperation(params: List<String>) =
            if (params.size == 9) {
                val durationInMillis = toLong(params[2])
                val connectivityTimeoutInMillis = toLong(params[3])
                val freshOnly = toBoolean(params[4]) ?: false
                val mergeOnNextOnError = toBoolean(params[5])
                val encrypt = toBoolean(params[6])
                val compress = toBoolean(params[7])
                val filterFinal = toBoolean(params[8]) ?: false

                when (params[1]) {
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
            } else null

}