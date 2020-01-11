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

package dev.pthomain.android.dejavu.injection

import android.content.ContentValues

internal fun mapToContentValues(map: Map<String, *>): ContentValues {
    val values = ContentValues()
    for ((key, value) in map) {
        when (value) {
            is Boolean -> values.put(key, value)
            is Float -> values.put(key, value)
            is Double -> values.put(key, value)
            is Long -> values.put(key, value)
            is Int -> values.put(key, value)
            is Byte -> values.put(key, value)
            is ByteArray -> values.put(key, value)
            is Short -> values.put(key, value)
            is String -> values.put(key, value)
        }
    }
    return values
}

interface Function1<T1, R> {
    fun get(t1: T1): R
}

interface Function2<T1, T2, R> {
    fun get(t1: T1, t2: T2): R
}

interface Function3<T1, T2, T3, R> {
    fun get(t1: T1, t2: T2, t3: T3): R
}

interface Function4<T1, T2, T3, T4, R> {
    fun get(t1: T1, t2: T2, t3: T3, t4: T4): R
}

interface Function6<T1, T2, T3, T4, T5, T6, R> {
    fun get(t1: T1, t2: T2, t3: T3, t4: T4, t5: T5, t6: T6): R
}