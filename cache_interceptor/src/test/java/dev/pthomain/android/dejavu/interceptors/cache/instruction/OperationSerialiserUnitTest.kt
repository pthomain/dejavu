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

import dev.pthomain.android.dejavu.interceptors.cache.instruction.CachePriority.*
import dev.pthomain.android.dejavu.interceptors.cache.instruction.Operation.*
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import junit.framework.TestCase.assertEquals
import org.junit.Test

class OperationSerialiserUnitTest {

    private fun getMap(targetClass: Class<*>) = targetClass.name.let {
        LinkedHashMap<String, Operation?>().apply {
            put("CACHE", Cache())
            put("DO_NOT_CACHE", DoNotCache)
            put("INVALIDATE", Invalidate())
            put("CLEAR", Clear())

            put("INVALIDATE:false", Invalidate())
            put("INVALIDATE:true", Invalidate(true))

            put("CLEAR:false:false", Clear())
            put("CLEAR:false:false", Clear(false))
            put("CLEAR:false:false", Clear(false, false))
            put("CLEAR:true:false", Clear(true, false))
            put("CLEAR:true:true", Clear(true, true))

            put("CACHE:DEFAULT:1234:5678:false:false", Cache(DEFAULT, 1234, 5678, false, false))
            put("CACHE:DEFAULT:1234:5678:false:true", Cache(DEFAULT, 1234, 5678, false, true))
            put("CACHE:DEFAULT:1234:5678:true:true", Cache(DEFAULT, 1234, 5678, true, true))
            put("CACHE:DEFAULT:1234:5678:true:false", Cache(DEFAULT, 1234, 5678, true, false))
            put("CACHE:DEFAULT::5678:true:true", Cache(DEFAULT, -1, 5678, true, true))
            put("CACHE:DEFAULT:::true:true", Cache(DEFAULT, -1, -1, true, true))

            put("CACHE:FRESH_PREFERRED:1234:5678:true:true", Cache(FRESH_PREFERRED, 1234, 5678, true, true))
            put("CACHE:FRESH_ONLY:1234:5678:true:true", Cache(FRESH_ONLY, 1234, 5678, true, true))
            put("CACHE:INVALIDATED:1234:5678:true:true", Cache(REFRESH_FRESH_PREFERRED, 1234, 5678, true, true))
            put("CACHE:INVALIDATED_FRESH_ONLY:1234:5678:true:true", Cache(REFRESH_FRESH_ONLY, 1234, 5678, true, true))
            put("CACHE:OFFLINE:1234:5678:true:true", Cache(OFFLINE, 1234, 5678, true, true))
            put("CACHE:OFFLINE_FRESH_ONLY:1234:5678:true:true", Cache(OFFLINE_FRESH_ONLY, 1234, 5678, true, true))
        }
    }

    @Test
    fun serialiseTopLevelClass() {
        serialise(TestResponse::class.java)
    }

    @Test
    fun deserialiseTopLevelClass() {
        deserialise(TestResponse::class.java)
    }

    @Test
    fun serialiseInnerClass() {
        serialise(Parent.InnerClass::class.java)
    }

    @Test
    fun deserialiseInnerClass() {
        deserialise(Parent.InnerClass::class.java)
    }

    interface Parent {
        class InnerClass
    }

    private fun serialise(targetClass: Class<*>) {
        getMap(targetClass).entries.forEachIndexed { index, entry ->
            assertEquals(
                    "${entry.value?.type} at position $index could not be serialised",
                    entry.key,
                    entry.value.toString()
            )
        }
    }

    private fun deserialise(targetClass: Class<*>) {
        getMap(targetClass).entries.forEachIndexed { index, entry ->
            assertEquals(
                    "${entry.value?.type} at position $index could not be deserialised",
                    entry.value,
                    Operation.fromString(entry.key)
            )
        }
    }

}