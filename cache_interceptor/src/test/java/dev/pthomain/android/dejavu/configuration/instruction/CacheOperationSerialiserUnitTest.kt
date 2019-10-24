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
import dev.pthomain.android.dejavu.test.network.model.TestResponse
import junit.framework.TestCase.assertEquals
import org.junit.Test

class CacheOperationSerialiserUnitTest {

    private fun getMap(targetClass: Class<*>) = targetClass.name.let {
        fun newCacheInstruction(operation: Operation) = CacheInstruction(targetClass, operation)

        LinkedHashMap<String, CacheInstruction>().apply {
            put("$it:DO_NOT_CACHE:", newCacheInstruction(DoNotCache))
            put("$it:INVALIDATE:", newCacheInstruction(Invalidate()))//TODO serialise null as Any
            put("$it:INVALIDATE:$it", newCacheInstruction(Invalidate(targetClass))) //TODO serialise null as Any
            put("$it:CLEAR:$it:true", newCacheInstruction(Clear(targetClass, true)))//TODO serialise null as Any
            put("$it:CLEAR:$it:false", newCacheInstruction(Clear(targetClass)))//TODO serialise null as Any
            put("$it:CLEAR:null:false", newCacheInstruction(Clear()))
            put("$it:CACHE:1234:4321:true:true:true:true:true", newCacheInstruction(Cache(1234L, 4321L, true, true, true, true, true)))
            put("$it:CACHE:1234:4321:true:true:true:true:false", newCacheInstruction(Cache(1234L, 4321L, true, true, true, true)))
            put("$it:CACHE:1234:4321:true:true:true:null:false", newCacheInstruction(Cache(1234L, 4321L, true, true, true)))
            put("$it:CACHE:1234:4321:true:true:null:null:false", newCacheInstruction(Cache(1234L, 4321L, true, true)))
            put("$it:CACHE:1234:4321:true:null:null:null:false", newCacheInstruction(Cache(1234L, 4321L, true)))
            put("$it:CACHE:1234:4321:false:null:null:null:false", newCacheInstruction(Cache(1234L, 4321L)))
            put("$it:CACHE:1234:null:false:null:null:null:false", newCacheInstruction(Cache(1234L)))
            put("$it:CACHE:null:null:false:null:null:null:false", newCacheInstruction(Cache()))
            put("$it:REFRESH:1234:4321:true:true:null:null:true", newCacheInstruction(Refresh(1234L, 4321L, true, true, true)))
            put("$it:REFRESH:1234:4321:true:true:null:null:false", newCacheInstruction(Refresh(1234L, 4321L, true, true)))
            put("$it:REFRESH:1234:4321:true:null:null:null:false", newCacheInstruction(Refresh(1234L, 4321L, true)))
            put("$it:REFRESH:1234:4321:false:null:null:null:false", newCacheInstruction(Refresh(1234L, 4321L)))
            put("$it:REFRESH:1234:null:false:null:null:null:false", newCacheInstruction(Refresh(1234L)))
            put("$it:REFRESH:null:null:false:null:null:null:false", newCacheInstruction(Refresh()))
            put("$it:OFFLINE:null:null:true:true:null:null:false", newCacheInstruction(Offline(true, true)))
            put("$it:OFFLINE:null:null:true:null:null:null:false", newCacheInstruction(Offline(true)))
            put("$it:OFFLINE:null:null:false:null:null:null:false", newCacheInstruction(Offline()))
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
                    "${entry.value.operation.type} at position $index could not be serialised",
                    entry.key,
                    entry.value.toString()
            )
        }
    }

    private fun deserialise(targetClass: Class<*>) {
        getMap(targetClass).entries.forEachIndexed { index, entry ->
            assertEquals(
                    "${entry.value.operation.type} at position $index could not be deserialised",
                    CacheOperationSerialiser().deserialise(entry.key),
                    entry.value
            )
        }
    }

}