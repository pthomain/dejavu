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

/**
 * Interface for custom serialisation
 */
interface Serialiser {

    /**
     * Whether the implementation can serialise objects of the given type.
     *
     * @param targetClass the given type to handle
     * @return whether or not this type is supported
     */
    fun canHandleType(targetClass: Class<*>): Boolean

    /**
     * Whether the implementation can deserialise the given string
     * (used for serialisers expecting a certain format, e.g a specific header)
     *
     * @param serialised the given string to deserialise
     * @return whether or not this string is supported
     */
    fun canHandleSerialisedFormat(serialised: String): Boolean

    /**
     * Serialises the given object, called only if canHandleType() returns true
     * for this object's class.
     *
     * @param target the object to serialise
     * @return the serialised object
     */
    fun <O : Any> serialise(target: O): String

    /**
     * Deserialises the given String into an object of the given type, called only if
     * canHandleSerialisedFormat() returns true for this given String.
     *
     * @param serialised the serialised String to deserialise
     * @param targetClass the type of the object represented by the serialised input
     */
    fun <O> deserialise(serialised: String,
                        targetClass: Class<O>): O

}