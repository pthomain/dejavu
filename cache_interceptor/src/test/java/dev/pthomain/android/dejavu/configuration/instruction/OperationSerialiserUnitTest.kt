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

class OperationSerialiserUnitTest {

    private fun getMap(targetClass: Class<*>) = targetClass.name.let {
        LinkedHashMap<String, Operation?>().apply {
            put("DO_NOT_CACHE", DoNotCache)
            put("INVALIDATE", null)
            put("INVALIDATE:$it", Invalidate())
            put("CLEAR", Clear(false, false))
            put("CLEAR:false:false", Clear(false, false))
            put("CLEAR:false", Clear(false, false))
            put("CLEAR:true", Clear(true, false))
            put("CLEAR:true:false", Clear(true, false))
            put("CLEAR:true:true", Clear(true, true))
//            put("CLEAR", Wipe) //FIXME there needs to be a flag for Wipe
            put("CACHE:1234:4321:true:true:true:true:true", Cache(1234L, 4321L, true, true, true, true, true))
            put("CACHE:1234:4321:true:true:true:true:false", Cache(1234L, 4321L, true, true, true, true))
            put("CACHE:1234:4321:true:true:true::false", Cache(1234L, 4321L, true, true, true))
            put("CACHE:1234:4321:true:true:::false", Cache(1234L, 4321L, true, true))
            put("CACHE:1234:4321:true::::false", Cache(1234L, 4321L, true))
            put("CACHE:1234:4321:false::::false", Cache(1234L, 4321L))
            put("CACHE:1234::false::::false", Cache(1234L))
            put("CACHE:::false::::false", Cache())
            put("REFRESH:1234:4321:true:true:::true", Refresh(1234L, 4321L, true, true, true))
            put("REFRESH:1234:4321:true:true:::false", Refresh(1234L, 4321L, true, true))
            put("REFRESH:1234:4321:true::::false", Refresh(1234L, 4321L, true))
            put("REFRESH:1234:4321:false::::false", Refresh(1234L, 4321L))
            put("REFRESH:1234::false::::false", Refresh(1234L))
            put("REFRESH:::false::::false", Refresh())
            put("OFFLINE:::true:true:::false", Offline(true, true))
            put("OFFLINE:::true::::false", Offline(true))
            put("OFFLINE:::false::::false", Offline())
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