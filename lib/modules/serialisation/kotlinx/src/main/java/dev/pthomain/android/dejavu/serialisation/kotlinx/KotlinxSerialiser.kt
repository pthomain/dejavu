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

package dev.pthomain.android.dejavu.serialisation.kotlinx

import dev.pthomain.android.dejavu.serialisation.SimpleSerialiser
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Custom Serialiser implementation using kotlinx.serialization.
 *
 * Uses reflective serializer lookup via [serializer] so that callers
 * can keep passing a plain [Class] reference.
 */
class KotlinxSerialiser(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SimpleSerialiser() {

    @Suppress("UNCHECKED_CAST")
    override fun <O : Any> serialise(target: O): String {
        val serializer = serializer(target.javaClass)
        return json.encodeToString(serializer, target)
    }

    override fun <O> deserialise(
        serialised: String,
        targetClass: Class<O>
    ): O {
        val serializer = serializer(targetClass)
        @Suppress("UNCHECKED_CAST")
        return json.decodeFromString(serializer, serialised) as O
    }
}
