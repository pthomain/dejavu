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

package uk.co.glass_software.android.dejavu.interceptors.internal.cache.serialisation

import android.net.Uri
import uk.co.glass_software.android.boilerplate.utils.log.Logger
import uk.co.glass_software.android.dejavu.interceptors.internal.cache.token.CacheToken
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

internal class Hasher(private val messageDigest: MessageDigest?) {

    fun getTokenKey(cacheToken: CacheToken): String =
            getParameters(cacheToken).let { parameters ->
                try {
                    parameters?.let { hash("${cacheToken.apiUrl}$$it") }
                            ?: hash(cacheToken.apiUrl)
                } catch (e: Exception) {
                    if (parameters == null)
                        cacheToken.apiUrl.hashCode().toString()
                    else
                        (cacheToken.apiUrl.hashCode() * 31 + parameters.hashCode()).toString()
                }
            }

    fun getParameters(cacheToken: CacheToken) =
            cacheToken.uniqueParameters?.let { params ->
                try {
                    Uri.parse("${cacheToken.apiUrl}?$params").let { uri ->
                        uri.queryParameterNames
                                .sorted()
                                .joinToString(separator = "&") {
                                    "$it=${uri.getQueryParameter(it)}"
                                }
                    }
                } catch (e: Exception) {
                    params
                }
            }

    @Throws(UnsupportedEncodingException::class)
    private fun hash(text: String): String =
            if (messageDigest == null) {
                var hash: Long = 7
                for (i in 0 until text.length) {
                    hash = hash * 31 + text[i].toLong()
                }
                hash.toString()
            } else {
                val textBytes = text.toByteArray(charset("UTF-8"))
                messageDigest.update(textBytes, 0, textBytes.size)
                bytesToString(messageDigest.digest() ?: textBytes)
            }

    private fun bytesToString(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    class Factory(private val logger: Logger) {

        fun create(): Hasher {
            var messageDigest = try {
                MessageDigest.getInstance("SHA-1").also {
                    logger.d(this, "Using SHA-1 hasher")
                }
            } catch (e: NoSuchAlgorithmException) {
                logger.e(this, "Could not create a SHA-1 message digest")
                null
            }

            if (messageDigest == null) {
                messageDigest = try {
                    MessageDigest.getInstance("MD5").also {
                        logger.d(this, "Using MD5 hasher")
                    }
                } catch (e: NoSuchAlgorithmException) {
                    logger.e(this, "Could not create a MD5 message digest")
                    null
                }
            }

            return Hasher(messageDigest)
        }
    }

    companion object {
        private val hexArray = "0123456789ABCDEF".toCharArray()
    }
}
