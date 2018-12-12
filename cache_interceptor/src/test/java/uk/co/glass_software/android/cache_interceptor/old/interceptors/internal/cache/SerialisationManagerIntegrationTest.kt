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

package uk.co.glass_software.android.cache_interceptor.old.interceptors.internal.cache


//class SerialisationManagerIntegrationTest : BaseIntegrationTest() {
//
//    private var stubbedResponse: TestResponse? = null
//
//    private var target: SerialisationManager<*>? = null
//
//    @Before
//    @Throws(Exception::class)
//    fun setUp() {
//        stubbedResponse = assetHelper.getStubbedResponse(TestResponse.STUB_FILE, TestResponse::class.java)
//                .blockingFirst()
//
//        target = SerialisationManager(
//                mock(Logger::class.java),
//                StoreEntryFactory.buildDefault(RuntimeEnvironment.applicationContext),
//                false,
//                true,
//                Gson()
//        )
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testCompress() {
//        val compressed = target!!.serialise(stubbedResponse)
//        assertEquals("Wrong compressed size", 2566, compressed!!.size)
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testUncompressSuccess() {
//        val compressed = target!!.serialise(stubbedResponse)
//        val mockAction = mock(Action::class.java)
//
//        val uncompressed = target!!.deserialise(TestResponse::class.java, compressed, mockAction)
//
//        stubbedResponse!!.metadata = null //no need to test metadata in this test
//        assertEquals("Responses didn't match", stubbedResponse, uncompressed)
//        verify<Any>(mockAction, never()).act()
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun testUncompressFailure() {
//        val compressed = target!!.serialise(stubbedResponse)
//        for (i in 0..49) {
//            compressed[i] = 0
//        }
//        val mockAction = mock(Action::class.java)
//
//        target!!.deserialise(TestResponse::class.java, compressed, mockAction)
//
//        verify<Any>(mockAction).act()
//    }
//}