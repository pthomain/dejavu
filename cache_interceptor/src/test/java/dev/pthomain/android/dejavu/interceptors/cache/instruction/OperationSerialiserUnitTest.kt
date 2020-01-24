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

import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.*
import dev.pthomain.android.dejavu.interceptors.cache.instruction.operation.CachePriority.*
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import junit.framework.TestCase.assertEquals
import org.junit.Test

class OperationSerialiserUnitTest {

    private fun getCommonMap(targetClass: Class<*>) = targetClass.name.let {
        LinkedHashMap<String, Operation?>().apply {
            put("DO_NOT_CACHE", DoNotCache)

            put("INVALIDATE:false", Invalidate())
            put("INVALIDATE:true", Invalidate(true))

            put("CLEAR:false:false", Clear())
            put("CLEAR:false:false", Clear(false))
            put("CLEAR:false:false", Clear(false, false))
            put("CLEAR:true:false", Clear(true, false))
            put("CLEAR:true:true", Clear(true, true))
            put("CLEAR:false:true", Clear(false, true))

            put("CACHE:DEFAULT:3600:::false:false", Cache())

            put("CACHE:FRESH_ONLY:1234:5678:7654:false:false", Cache(FRESH_ONLY, 1234, 5678, 7654, false, false))
            put("CACHE:FRESH_PREFERRED:1234:5678:7654:true:true", Cache(FRESH_PREFERRED, 1234, 5678, 7654, true, true))

            put("CACHE:REFRESH:1234:5678:7654:false:true", Cache(REFRESH, 1234, 5678, 7654, false, true))
            put("CACHE:REFRESH_FRESH_ONLY:1234:5678:7654:true:false", Cache(REFRESH_FRESH_ONLY, 1234, 5678, 7654, true, false))

            put("CACHE:FRESH_PREFERRED:1234:5678:7654:true:true", Cache(FRESH_PREFERRED, 1234, 5678, 7654, true, true))
            put("CACHE:FRESH_ONLY:1234:5678:7654:true:true", Cache(FRESH_ONLY, 1234, 5678, 7654, true, true))

            put("CACHE:REFRESH_FRESH_PREFERRED:1234:5678:7654:true:true", Cache(REFRESH_FRESH_PREFERRED, 1234, 5678, 7654, true, true))
            put("CACHE:REFRESH_FRESH_ONLY:1234:5678:7654:true:true", Cache(REFRESH_FRESH_ONLY, 1234, 5678, 7654, true, true))

            put("CACHE:OFFLINE:1234:5678:7654:true:true", Cache(OFFLINE, 1234, 5678, 7654, true, true))
            put("CACHE:OFFLINE_FRESH_ONLY:1234:5678:7654:true:true", Cache(OFFLINE_FRESH_ONLY, 1234, 5678, 7654, true, true))
        }
    }

    private fun getSerialisationMap(targetClass: Class<*>) = getCommonMap(targetClass).apply {
        put("CACHE:DEFAULT:3600:::true:true", Cache(DEFAULT, -1, -1, -1, true, true))
        put("CACHE:REFRESH_FRESH_PREFERRED:3600:5678:7654:true:true", Cache(REFRESH_FRESH_PREFERRED, -1, 5678, 7654, true, true))
    }

    private fun getDeserialisationMap(targetClass: Class<*>) = getCommonMap(targetClass).apply {
        put("CACHE:DEFAULT:3600:::true:true", Cache(DEFAULT, 3600, null, null, true, true))
        put("CACHE:REFRESH_FRESH_PREFERRED:3600:5678:7654:true:true", Cache(REFRESH_FRESH_PREFERRED, 3600, 5678, 7654, true, true))
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
        getSerialisationMap(targetClass).entries.forEachIndexed { index, entry ->
            assertEquals(
                    "${entry.value?.type} at position $index could not be serialised",
                    entry.key,
                    entry.value.toString()
            )
        }
    }

    private fun deserialise(targetClass: Class<*>) {
        getDeserialisationMap(targetClass).entries.forEachIndexed { index, entry ->
            assertEquals(
                    "${entry.value?.type} at position $index could not be deserialised",
                    entry.value,
                    Operation.fromString(entry.key)
            )
        }
    }

}