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

package dev.pthomain.android.dejavu.persistence.base

import dev.pthomain.android.dejavu.persistence.base.CacheDataHolder.Incomplete
import dev.pthomain.android.dejavu.persistence.base.store.KeyValueStore
import org.junit.Before


abstract class BaseKeyValueStoreUnitTest<S : KeyValueStore<String, String, Incomplete>> {

    private lateinit var target: S

    @Before
    fun setUp() {
//        target = setUpTarget()
    }

    abstract fun setUpTarget(): S

//    fun testFindPartialKey() {
//
//
//    }
//
//    abstract fun testFindPartialKey()

    fun get() {

    }

//    abstract fun get(hasResult: Boolean)

//    fun save()
//
//    fun values(): Map<K, V>
//
//    fun delete(key: K)
//
//    fun rename(oldKey: K, newKey: K)

}