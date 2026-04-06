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

package dev.pthomain.android.dejavu.demo

import android.util.Log
import dev.pthomain.android.dejavu.utils.Logger

/**
 * Simple Logger implementation that delegates to Android's Log.
 */
class AndroidLogger(private val tag: String) : Logger {

    override fun d(tagOrCaller: Any, message: String) {
        Log.d(deriveTag(tagOrCaller), message)
    }

    override fun e(tagOrCaller: Any, message: String) {
        Log.e(deriveTag(tagOrCaller), message)
    }

    override fun e(tagOrCaller: Any, t: Throwable, message: String?) {
        Log.e(deriveTag(tagOrCaller), message ?: t.message ?: "Error", t)
    }

    private fun deriveTag(tagOrCaller: Any): String =
            when (tagOrCaller) {
                is String -> tagOrCaller
                else -> tagOrCaller::class.java.simpleName.take(23).ifEmpty { tag }
            }
}
