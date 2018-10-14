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

package uk.co.glass_software.android.cache_interceptor.interceptors.internal.cache.serialisation

import java.io.UnsupportedEncodingException
import java.security.MessageDigest

internal class Hasher(private val messageDigest: MessageDigest?)
    : (ByteArray) -> String {

    @Throws(UnsupportedEncodingException::class)
    fun hash(text: String) = if (messageDigest == null) {
        var hash: Long = 7
        for (i in 0 until text.length) {
            hash = hash * 31 + text[i].toLong()
        }
        hash.toString()
    } else {
        val textBytes = text.toByteArray(charset("UTF-8"))
        messageDigest.update(textBytes, 0, textBytes.size)
        invoke(messageDigest.digest())
    }

    override fun invoke(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    companion object {
        private val hexArray = "0123456789ABCDEF".toCharArray()
    }
}
