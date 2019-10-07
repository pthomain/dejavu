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

package dev.pthomain.android.dejavu.interceptors.internal.cache.serialisation

import android.net.Uri
import dev.pthomain.android.boilerplate.core.utils.log.Logger
import dev.pthomain.android.dejavu.interceptors.internal.cache.metadata.RequestMetadata
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

//TODO JavaDoc
internal class Hasher(private val logger: Logger,
                      private val messageDigest: MessageDigest?,
                      private val uriParser: (String) -> Uri) {

    fun hash(requestMetadata: RequestMetadata.UnHashed): RequestMetadata.Hashed? {
        val uri = uriParser(requestMetadata.url)
        val sortedParameters = getSortedParameters(uri)

        val sortedUrl = getSortedUrl(
                uri,
                sortedParameters
        )

        val urlAndBody = requestMetadata.requestBody?.let { "$sortedUrl||$it" } ?: sortedUrl

        val urlHash = try {
            hash(urlAndBody)
        } catch (e: Exception) {
            logger.e(this, e, "Could not hash URL and body")
            return null
        }

        val classHash = try {
            hash(requestMetadata.responseClass.name)
        } catch (e: Exception) {
            logger.e(this, e, "Could not hash response class")
            return null
        }

        return RequestMetadata.Hashed(
                requestMetadata.responseClass,
                requestMetadata.url,
                requestMetadata.requestBody,
                urlHash,
                classHash
        )
    }

    private fun getSortedUrl(url: Uri,
                             sortedParameters: String) = with(url) {
        "$scheme:$host$path?$sortedParameters"
    }

    internal fun getSortedParameters(uri: Uri) =
            uri.queryParameterNames
                    .sorted()
                    .joinToString(separator = "&") {
                        "$it=${uri.getQueryParameter(it)}"
                    }

    @Throws(UnsupportedEncodingException::class)
    fun hash(text: String) =
            if (messageDigest == null) {
                var hash: Long = 7
                for (element in text) {
                    hash = hash * 31 + element.toLong()
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

    class Factory(private val logger: Logger,
                  private val uriParser: (String) -> Uri) {

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

            return Hasher(
                    logger,
                    messageDigest,
                    uriParser
            )
        }
    }

    companion object {
        private val hexArray = "0123456789ABCDEF".toCharArray()
    }
}
